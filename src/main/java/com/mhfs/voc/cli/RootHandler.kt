package com.mhfs.voc.cli

import com.mhfs.voc.VocabularyService
import java.io.PrintWriter

class RootHandler(private val service: VocabularyService): TerminalHandler {
    override fun handleInput(input: String, writer: PrintWriter): TerminalHandler? {
        when (input) {
            ":q" -> return null
            ":h", "help" -> printHelp(writer)
            "activate" -> return ActivationInitHandler(this, service)
            "session" -> return QuestionInitHandler(this, service)
            "statistics" -> writer.println("In Development") //TODO: Implement
        }
        return this
    }

    private fun printHelp(writer: PrintWriter) {
        defaultHelp(writer)
        writer.println("The following options are available:")
        writer.println("activate        Activate vocabulary")
        writer.println("session         Start a vocabulary session")
        writer.println("statistics      Display statistics")
    }

    override fun getLeftPrompt() = ""

    override fun getRightPrompt() = ""

    override fun handleInterrupt(writer: PrintWriter): TerminalHandler? = null
}