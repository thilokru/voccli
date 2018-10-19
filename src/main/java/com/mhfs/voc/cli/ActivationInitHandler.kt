package com.mhfs.voc.cli

import com.mhfs.voc.VocabularyService
import java.io.PrintWriter
import java.lang.NumberFormatException

class ActivationInitHandler(private val parent: TerminalHandler, private val service: VocabularyService): TerminalHandler {

    private var step = 1
    private var count: Int = 0
    private lateinit var selector: String

    override fun handleInput(input: String, writer: PrintWriter): TerminalHandler? {
        if (input == ":q") return parent
        if (input == ":h") {
            printHelp(writer)
            return this
        }
        when(step) {
            1 -> {
                try {
                    count = input.toInt()
                    step = 2
                } catch (e: NumberFormatException) {
                    writer.println("This is not a number.")
                }
                return this
            }
            2 -> {
                try {
                    service.createActivationSession(count, input)
                } catch (e: IllegalArgumentException) {
                    writer.println("An error occurred parsing the selector:")
                    e.printStackTrace(writer)
                    return this
                }
                return QuestionHandler(parent, service, isActivation = true)
            }
        }
        return this
    }

    private fun printHelp(writer: PrintWriter) {
        defaultHelp(writer)
        when (step) {
            1 -> writer.println("Please enter the amount of words to activate.")
            2 -> writer.println("Please enter a selector. See documentation.")
        }
    }

    override fun getLeftPrompt() = "activation " + if(step == 1) "count" else "selector"

    override fun getRightPrompt() = "$step/$TOTAL_STEPS"

    override fun handleInterrupt(writer: PrintWriter): TerminalHandler? = parent

    companion object {
        private const val TOTAL_STEPS = 2
    }
}