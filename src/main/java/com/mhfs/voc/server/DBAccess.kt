package com.mhfs.voc.server

import com.mhfs.voc.VocabularyService
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*
import java.util.Date as UDate
import java.sql.Date as SDate

class DBAccess(val sqlPrefix: String, private val connection: Connection) {

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
    private val pAssociationWrite = prepare("/$sqlPrefix/write_association.sql")
    private val pDateWrite = prepare("/$sqlPrefix/write_date.sql")
    private val pWriteActive = prepare("/$sqlPrefix/write_activation.sql")

    fun getVocabulary(maxCount: Int): MutableList<DBQuestion> {
        pVocabularySelect.setDate(1, SDate(UDate().time))
        pVocabularySelect.setInt(2, maxCount)
        val results = pVocabularySelect.executeQuery()
        val vocabularyList = LinkedList<DBQuestion>()
        while (results.next()) {
            val associatedData = HashMap<String, String>()
            pAssociationRetrieval.setInt(1, results.getInt(1))
            val asResult = pAssociationRetrieval.executeQuery()
            while (asResult.next()) associatedData[asResult.getString(1)] = asResult.getString(2)
            vocabularyList += DBQuestion(results.getInt(1), results.getString(2), results.getString(3),
                    results.getString(4), results.getString(5), associatedData)
        }
        return vocabularyList
    }

    fun getVocabularyForActivation(maxCount: Int): MutableList<DBQuestion> {
        pActivationSelect.setInt(1, maxCount)
        val results = pActivationSelect.executeQuery()
        val vocabularyList = LinkedList<DBQuestion>()
        while (results.next()) {
            val associatedData = HashMap<String, String>()
            pAssociationRetrieval.setInt(1, results.getInt(1))
            val asResult = pAssociationRetrieval.executeQuery()
            while (asResult.next()) associatedData[asResult.getString(1)] = asResult.getString(2)
            vocabularyList += DBQuestion(results.getInt(1), results.getString(2), results.getString(3),
                    results.getString(4), results.getString(5), associatedData)
        }
        return vocabularyList
    }

    fun updateQuestion(question: DBQuestion, nextDue: UDate) {
        question.associatedData.forEach{
            pAssociationWrite.setInt(1, question.id)
            pAssociationWrite.setString(2, it.key)
            pAssociationWrite.setString(3, it.value)
            pAssociationWrite.executeUpdate()
        }
        pDateWrite.setDate(1, SDate(nextDue.time))
        pDateWrite.setInt(2, question.id)
        pDateWrite.executeUpdate()
    }

    fun activate(question: DBQuestion) {
        pWriteActive.setInt(1, question.id)
        pWriteActive.executeUpdate()
        question.associatedData.forEach{
            pAssociationWrite.setInt(1, question.id)
            pAssociationWrite.setString(2, it.key)
            pAssociationWrite.setString(3, it.value)
            pAssociationWrite.executeUpdate()
        }
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

    class DBQuestion(val id: Int, question: String, questionLanguage: String,
                     solution: String?, targetLanguage: String,
                     associatedData: MutableMap<String, String>):
            VocabularyService.Question(question, questionLanguage, solution, targetLanguage, associatedData)
}