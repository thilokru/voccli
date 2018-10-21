package com.mhfs.voc.cli

import com.mhfs.voc.VocabularyService
import java.io.PrintWriter
import java.lang.NumberFormatException

class ActivationInitHandler(private val parent: TerminalHandler, private val service: VocabularyService): TerminalHandler {

    private var count: Int = 0
    private lateinit var selector: String

    override fun handleInput(input: String, writer: PrintWriter): TerminalHandler? {
        if (input == ":q") return parent
        if (input == ":h") {
            printHelp(writer)
            return this
        }
        try {
            count = input.toInt()
            val state = service.createSession(VocabularyService.SessionDescription(true, count))
            if (state.remainingQuestions == 0) {
                writer.println("No questions.")
                return parent
            }
        } catch (e: NumberFormatException) {
            writer.println("This is not a number.")
            return this
        }
        return QuestionHandler(parent, service)
    }

    private fun printHelp(writer: PrintWriter) {
        defaultHelp(writer)
        writer.println("Please enter the amount of words to activate.")
    }

    override fun getLeftPrompt() = "activation count"

    override fun getRightPrompt() = ""

    override fun handleInterrupt(writer: PrintWriter): TerminalHandler? = parent

    companion object {
        private const val TOTAL_STEPS = 2
    }
}