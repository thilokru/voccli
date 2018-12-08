package com.mhfs.voc.server

import com.mhfs.voc.VocabularyService
import io.ktor.auth.UserPasswordCredential
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.security.SecureRandom
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*
import java.util.Date as UDate
import java.sql.Date as SDate
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec



class DBAccess(val sqlPrefix: String, private val connection: Connection) {

    companion object {
        const val HASHING_ITERATIONS = 65536
        const val HASH_LENGTH = 512
        const val SALT_LENGTH = 16
    }

    init {
        val statement = connection.createStatement()
        try {
            statement.executeUpdate(sqlFromResource("/$sqlPrefix/db_setup.sql"))
        } catch (e: SQLException) {
            println("Error setting up database.")
            throw RuntimeException(e)
        } catch (e: IOException) {
            println("Couldn't read setup file.")
            throw RuntimeException(e)
        }
        Runtime.getRuntime().addShutdownHook(Thread(::terminate))
    }

    private val pVocabularySelect = prepare("/$sqlPrefix/select_active_content.sql")
    private val pActivationSelect = prepare("/$sqlPrefix/select_inactive_content.sql")
    private val pAssociationRetrieval = prepare("/$sqlPrefix/select_associated_data.sql")
    private val pDateWrite = prepare("/$sqlPrefix/write_phase.sql")
    private val pWriteActive = prepare("/$sqlPrefix/write_activation.sql")
    private val pGetPassword = prepare("/$sqlPrefix/get_password_hash.sql")
    private val pCreateUser = prepareIdReturn("/$sqlPrefix/create_user.sql")

    private val secureRandom = SecureRandom()

    fun getVocabulary(userID: Int, maxCount: Int): MutableList<DBQuestion> {
        pVocabularySelect.setInt(1, userID)
        pVocabularySelect.setDate(2, SDate(UDate().time))
        pVocabularySelect.setInt(3, maxCount)
        val results = pVocabularySelect.executeQuery()
        val vocabularyList = LinkedList<DBQuestion>()
        while (results.next()) {
            val associatedData = HashMap<String, String>()
            pAssociationRetrieval.setInt(1, results.getInt(1))
            val asResult = pAssociationRetrieval.executeQuery()
            while (asResult.next()) associatedData[asResult.getString(1)] = asResult.getString(2)
            vocabularyList += DBQuestion(results.getInt(1), userID, results.getString(2),
                    results.getString(3), results.getString(4), results.getString(5),
                    results.getInt(6), associatedData)
        }
        return vocabularyList
    }

    fun getVocabularyForActivation(userID: Int, maxCount: Int): MutableList<DBQuestion> {
        pActivationSelect.setInt(1, userID)
        pActivationSelect.setInt(2, maxCount)
        val results = pActivationSelect.executeQuery()
        val vocabularyList = LinkedList<DBQuestion>()
        while (results.next()) {
            val associatedData = HashMap<String, String>()
            pAssociationRetrieval.setInt(1, results.getInt(1))
            val asResult = pAssociationRetrieval.executeQuery()
            while (asResult.next()) associatedData[asResult.getString(1)] = asResult.getString(2)
            vocabularyList += DBQuestion(results.getInt(1), userID, results.getString(2),
                    results.getString(3), results.getString(4), results.getString(5),
                    0, associatedData)
        }
        return vocabularyList
    }

    fun updateQuestion(question: DBQuestion, nextDue: UDate) {
        pDateWrite.setInt(1, question.phase)
        pDateWrite.setDate(2, SDate(nextDue.time))
        pDateWrite.setInt(3, question.uid)
        pDateWrite.setInt(4, question.id)
        pDateWrite.executeUpdate()
    }

    fun activate(question: DBQuestion) {
        pWriteActive.setInt(1, question.uid)
        pWriteActive.setInt(2, question.id)
        pWriteActive.setInt(3, question.phase)
        pWriteActive.executeUpdate()
    }

    /**
     * Uses the {@see hash} method to calculate the salted hash.
     * @return a user id if the credentials are valid, null otherwise
     */
    fun authenticate(credentials: UserPasswordCredential): Int? {
        //Note, that credentials.name is unfiltered user input.
        //This is a prepared statement. SQL-Injections should not be possible.
        pGetPassword.setString(1, credentials.name)
        val result = pGetPassword.executeQuery()
        if (result.next()) {
            //To hash the password, we need the salt.
            val salt = result.getBytes(2)
            //We calculate the hash
            val hash = hash(credentials.password, salt)
            //If the hashes match...
            return if (hash.contentEquals(result.getBytes(3))) {
                //then return the user id
                result.getInt(1)
            } else {
                //Otherwise the user could not be authenticated and we return null
                null
            }
        } else {
            //There was no user with that name. However, the method returns null, just like if the password is wrong,
            //so that a potential attacker can't differentiate between 'no such user' and 'wrong password'
            return null
        }
    }

    /**
     * @return The newly created user id or null, if a name collision occurred
     */
    fun register(user: String, password: String): Int? {
        //Note, that user may be unfiltered user input.
        //This is a prepared statement. SQL-Injections should not be possible.
        pGetPassword.setString(1, user)
        val result = pGetPassword.executeQuery()
        if (result.next()) {
            //This username already exists.
            return null
        }
        //First, we need a nice salt. We use a cryptographic RNG
        val salt = ByteArray(SALT_LENGTH)
        secureRandom.nextBytes(salt)

        //Retrieve the password hash
        val hash = hash(password, salt)
        //Store the user data and get the newly generated user id.
        pCreateUser.setString(1, user)
        pCreateUser.setBytes(2, salt)
        pCreateUser.setBytes(3, hash)
        pCreateUser.execute()
        val keys = pCreateUser.generatedKeys
        //We want the first key
        keys.next()
        //Return the newly generated id.
        return keys.getInt(1)
    }

    /**
     * PBKDF2 based password hashing.
     * @param password The plain text password to be hashed
     * @param salt The salt
     * @return The base64 encoded password hash
     */
    private fun hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, HASHING_ITERATIONS, HASH_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        return factory.generateSecret(spec).encoded
    }

    fun readConfiguration(session: UserSession) {

    }

    private fun prepare(resource: String) = connection.prepareStatement(sqlFromResource(resource))

    private fun prepareIdReturn(resource: String) = connection.prepareStatement(sqlFromResource(resource), PreparedStatement.RETURN_GENERATED_KEYS)

    private fun sqlFromResource(resource: String): String {
        val reader = BufferedReader(InputStreamReader(this.javaClass.getResourceAsStream(resource)))
        return reader.lineSequence().joinToString(separator = " "){ it }
    }

    fun disconnect() {
        if (!connection.autoCommit)
            connection.commit()
        connection.close()
    }


    private fun terminate() {
        if(!connection.isClosed) {
            if (!connection.autoCommit)
                connection.rollback()
            connection.close()
        }
    }

    class DBQuestion(val id: Int, val uid: Int, question: String, questionLanguage: String,
                     solution: String?, targetLanguage: String, var phase: Int,
                     associatedData: MutableMap<String, String>):
            VocabularyService.Question(question, questionLanguage, solution, targetLanguage, associatedData)
}