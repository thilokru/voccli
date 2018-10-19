package com.mhfs.voc.server

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

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
}