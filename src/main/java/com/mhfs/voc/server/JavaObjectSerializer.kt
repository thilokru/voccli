package com.mhfs.voc.server

import io.ktor.sessions.SessionSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*

class JavaObjectSerializer: SessionSerializer {
    override fun deserialize(text: String): Any {
        val bytes = Base64.getUrlDecoder().decode(text)
        val input = ObjectInputStream(ByteArrayInputStream(bytes))
        return input.readObject()
    }

    override fun serialize(session: Any): String {
        val baos = ByteArrayOutputStream()
        val out = ObjectOutputStream(baos)
        out.writeObject(session)
        out.flush()
        return Base64.getUrlEncoder().encodeToString(baos.toByteArray())
    }
}