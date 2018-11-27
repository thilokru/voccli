package com.mhfs.voc.server

import com.mhfs.voc.ManifestVersionProvider
import com.mhfs.voc.VocabularyService
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.*
import io.ktor.client.request.forms.formData
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.default
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.*
import io.ktor.util.KtorExperimentalAPI
import picocli.CommandLine
import java.io.File
import java.lang.IllegalArgumentException
import java.sql.DriverManager
import java.text.DateFormat
import java.time.Duration
import java.util.*


@CommandLine.Command(name = "vocserv", description = ["A vocabulary server tool."], mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider::class)
class ServerStarter(defaultLocation: String = "voc_db") : Runnable {

    @CommandLine.Parameters(arity = "1", description = ["H2 data base location"], paramLabel = "FILE")
    private var dbLocation: String = defaultLocation

    private lateinit var apiProvider: ServerAPIProvider

    @KtorExperimentalAPI
    override fun run() {
        val connection = DriverManager.getConnection("jdbc:h2:$dbLocation")
                ?: throw NullPointerException("DB not accessible.")
        val access = DBAccess("h2", connection)
        apiProvider = ServerAPIProvider(access)


        val server = embeddedServer(Netty, port = 8080, host = "localhost") {
            install(DefaultHeaders)
            install(Compression)
            install(CallLogging)
            install(ContentNegotiation) {
                gson {
                    setDateFormat(DateFormat.LONG)
                    setPrettyPrinting()
                }
                formData()
            }
            install(StatusPages) {
                exception<IllegalStateException> {cause ->
                    cause.printStackTrace()
                    call.respond(HttpStatusCode.Conflict, "Conflict")
                    throw cause
                }
                exception<IllegalArgumentException> { cause ->
                    cause.printStackTrace()
                    call.respond(HttpStatusCode.RequestTimeout, "Request Timeout")
                    throw cause
                }
                exception<Throwable> { cause ->
                    val time = Date().time
                    println("Error occurred at $time")
                    call.respond(HttpStatusCode.InternalServerError, "Error time: $time")
                    throw cause
                }
            }
            install(Sessions) {
                cookie<UserSession>("SESSION_ID", //SESSION storage not working?
                        storage = SessionStorageMemory()) {
                    cookie.path = "/"
                    cookie.duration = Duration.ofHours(1)
                    serializer = JavaObjectSerializer()
                }
            }
            install(Authentication) {
                form("api-user") {
                    userParamName = "user"
                    passwordParamName = "password"
                    challenge = FormAuthChallenge.Redirect { "/index.html" }
                    skipWhen { call -> call.sessions.get<UserSession>() != null }
                    validate { credentials ->
                        //If the user has a session, he is authenticated.
                        var session = sessions.get<UserSession>()
                        if (session != null) return@validate session

                        //Otherwise, check username and password, and load settings
                        val uid = access.authenticate(credentials)
                        return@validate if (uid != null) {
                            session = UserSession(credentials.name, uid)
                            access.readConfiguration(session)
                            sessions.set(session)
                            session
                        } else null
                    }
                }
            }


            routing {
                trace { application.log.trace(it.buildText()) }
                default("index.html")
                route("/api") {
                    route("v1") {
                        authenticate("api-user") {
                            accept(ContentType.Application.Json) {
                                post<VocabularyService.SessionDescription>("session") { req ->
                                    call.respond(apiProvider.createSession(req, call.sessions.get<UserSession>()!!))
                                }
                                post<Boolean>("correction") { correct ->
                                    call.respond(apiProvider.correction(correct, call.sessions.get<UserSession>()!!))
                                }
                            }
                            accept(ContentType.Text.Plain) {
                                post<String>("answer") { answer ->
                                    call.respond(apiProvider.answer(answer, call.sessions.get<UserSession>()!!))
                                }
                            }
                            get("state") {
                                call.respond(apiProvider.getState(call.sessions.get<UserSession>()!!))
                            }
                            post("cancel") {
                                call.respond(apiProvider.cancelSession(call.sessions.get<UserSession>()!!))
                            }
                        }
                    }
                }
                authenticate("api-user") {
                    post("/login") {
                        call.respondRedirect("/session.html", permanent = true)
                    }
                }

                static("/") {
                    resource("index.html", "web/index.html")
                    resource("style.css", "web/style.css")
                    authenticate("api-user") {
                        resource("session.html", "web/session.html")
                        resource("main.js", "web/main.js")
                    }
                }
            }
        }
        server.start()
    }
}

fun main(args: Array<String>) {
    CommandLine.run(ServerStarter(), *args)
}