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
import io.ktor.http.Parameters
import io.ktor.http.content.*
import io.ktor.request.receiveMultipart
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

    @CommandLine.Option(names = ["--debug-user"], description = ["Provides a debug user. Don't use in production."])
    private var hasDebugUser = false

    private lateinit var apiProvider: ServerAPIProvider

    @KtorExperimentalAPI
    override fun run() {
        val connection = DriverManager.getConnection("jdbc:h2:$dbLocation")
                ?: throw NullPointerException("DB not accessible.")
        val access = DBAccess("h2", connection)
        apiProvider = ServerAPIProvider(access)

        if (hasDebugUser) {
            access.register("debug", "123456")
        }

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
                    //If the user is not authenticated, redirect him to login.html
                    challenge = FormAuthChallenge.Redirect { "/login.html" }
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
                //Redirect user to index.html
                get("/") {
                    call.respondRedirect("/index.html", permanent = true)
                }
                //Specify api locations
                route("/api") {
                    route("v1") {
                        //Only authenticated users may create sessions
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
                //Login method. Authentication parses form data.
                authenticate("api-user") {
                    post("/login") {
                        call.respondRedirect("/session.html", permanent = true)
                    }
                }
                //Registration method.
                post<Parameters>("/register") {
                    val user = it["user"]
                    val password = it["password"]
                    if (user == null || password == null) {
                        call.respond(HttpStatusCode.ExpectationFailed, "User and password need to be specified!")
                        return@post
                    }
                    //Register user
                    val uid = access.register(user, password)
                    if (uid == null) {
                        call.respondRedirect("/register.html?exists=true", permanent = false)
                        return@post
                    }
                    //Create session
                    val session = UserSession(user, uid)
                    access.readConfiguration(session)
                    call.sessions.set(session)
                    //Redirect user
                    call.respondRedirect("/index.html", permanent = false)
                }

                //We will deploy this application as a jar file, pulling resources, not files.
                static("/") {
                    resource("index.html", "web/index.html")
                    resource("login.html", "web/login.html")
                    resource("register.html", "web/register.html")
                    resource("style.css", "web/style.css")
                    resource("login-style.css", "web/login-style.css")
                    resource("index-style.css", "web/index-style.css")
                    resource("session-style.css", "web/session-style.css")
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