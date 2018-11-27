package com.mhfs.voc.server

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.ktor.sessions.SessionSerializer
import java.util.*

class GsonSessionSerializer(val type: java.lang.reflect.Type,
                            configure: GsonBuilder.() -> Unit = {}) : SessionSerializer {
    private val gson: Gson
    init {
        val builder = GsonBuilder()
        builder.registerTypeAdapter(String.javaClass, object: TypeAdapter<String>() {
            override fun write(out: JsonWriter, value: String) {
                out.value(Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8)))
            }

            override fun read(`in`: JsonReader) = String(Base64.getDecoder().decode(`in`.nextString()), Charsets.UTF_8)
        })
        configure(builder)
        gson = builder.create()
    }

    override fun serialize(session: Any): String = gson.toJson(session)
    override fun deserialize(text: String): Any {
        println(text)
        return gson.fromJson(text, type)
    }
}
