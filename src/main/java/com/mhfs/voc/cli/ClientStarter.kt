package com.mhfs.voc.cli

import com.mhfs.voc.ManifestVersionProvider
import com.mhfs.voc.VocabularyService
import com.mhfs.voc.server.ServerStarter
import picocli.CommandLine
import picocli.CommandLine.*
import java.awt.Desktop
import java.io.File
import java.net.URI
import kotlin.concurrent.thread

@Command(name = "voccli", description = ["A CLI vocabulary tool."], mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider::class)
class ClientStarter: Runnable {

    @Option(description = ["Which endpoint to connect to."], names = ["--endpoint"], paramLabel = "(LOCAL|WEB)")
    private var endPoint = EndPoint.WEB

    @Option(description = ["Points to a file with data to load."], names = ["--load"], paramLabel = "FILE")
    private var loadLocation: File? = null

    @Parameters(description = ["The endpoint descriptor, can be a URL or a DB-File."], arity = "1", paramLabel = "RESOURCE")
    private var target = "voc_db"

    override fun run() {
        val service = endPoint.connect(target, loadLocation)
        /*if (endPoint == EndPoint.BROWSER)
            return*/
        CLI(RootHandler(service)).start()
    }

    enum class EndPoint {
        /*LOCAL {
            override fun connect(target: String, loadLocation: File?): VocabularyService {
                val server = ServerStarter(defaultLocation = target, loadLocation = loadLocation)
                server.run()
                return server.getService()
            }
        }, */WEB {
            override fun connect(target: String, loadLocation: File?): VocabularyService {
                return RESTService(target)
            }
        }/*, BROWSER {
            override fun connect(target: String, loadLocation: File?): VocabularyService {
                val server = ServerStarter(defaultLocation = target, loadLocation = loadLocation, httpServer = true)
                server.run()
                Desktop.getDesktop().browse(URI("http://localhost:8080/index.html"))
                return server.getService()
            }
        }*/;
        abstract fun connect(target: String, loadLocation: File?): VocabularyService
    }
}

fun main(args: Array<String>) {
    CommandLine.run(ClientStarter(), *args)
}