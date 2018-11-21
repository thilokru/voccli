package com.mhfs.voc.server

import com.mhfs.voc.ManifestVersionProvider
import com.mhfs.voc.VocabularyService
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import picocli.CommandLine
import java.io.File
import java.lang.IllegalArgumentException
import java.sql.DriverManager
import java.text.DateFormat


@CommandLine.Command(name = "vocserv", description = ["A vocabulary server tool."], mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider::class)
class ServerStarter(defaultLocation: String = "voc_db", loadLocation: File? = null,
                    val httpServer: Boolean = false): Runnable {

    @CommandLine.Option(description = ["Points to a file with data to load."], names = ["--load"], paramLabel = "FILE")
    private var dbLocation: String = defaultLocation

    private lateinit var apiProvider: ServerAPIProvider

    override fun run() {
        val connection = DriverManager.getConnection("jdbc:h2:$dbLocation") ?: throw NullPointerException("DB not accessible.")
        apiProvider = ServerAPIProvider(DBAccess("h2", connection), arrayOf(0, 1, 3, 7, 14, 60))

        if (httpServer) {
            val server = embeddedServer(Netty, port = 8080, host = "localhost") {
                install(DefaultHeaders)
                install(Compression)
                install(CallLogging)
                install(ContentNegotiation) {
                    gson {
                        setDateFormat(DateFormat.LONG)
                        setPrettyPrinting()
                    }
                }
                install(StatusPages) {
                    exception<IllegalStateException> { cause ->
                        call.respond(HttpStatusCode.Conflict)
                    }
                    exception<IllegalArgumentException> { cause ->
                        call.respond(HttpStatusCode.RequestTimeout)
                    }
                }

                routing {
                    get("/") {
                        call.respondRedirect("index.html", permanent = true)
                    }
                    route("/api") {
                        route("v1") {
                            accept(ContentType.Application.Json) {
                                post<VocabularyService.SessionDescription>("session") { req ->
                                    call.respond(apiProvider.createSession(req))
                                }
                                post<Boolean>("correction") { correct ->
                                    call.respond(apiProvider.correction(correct))
                                }
                            }
                            accept(ContentType.Text.Plain) {
                                post<String>("answer") { answer->
                                    call.respond(apiProvider.answer(answer))
                                }
                            }
                            get("state") {
                                call.respond(apiProvider.getState())
                            }
                            post("cancel") {
                                call.respond(apiProvider.cancelSession())
                            }
                        }
                    }
                    static("/") {
                        resources("web")
                    }
                }
            }
            server.start()
        }
    }

    fun getService(): VocabularyService = apiProvider
}

fun main(args: Array<String>) {
    CommandLine.run(ServerStarter(httpServer = true), *args)
}