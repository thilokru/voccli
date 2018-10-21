package com.mhfs.voc.cli

import com.mhfs.voc.VocabularyService
import java.io.PrintWriter
import java.lang.NumberFormatException

class QuestionInitHandler(private val parent: TerminalHandler, private val service: VocabularyService): TerminalHandler {
    override fun handleInput(input: String, writer: PrintWriter): TerminalHandler? {
        if (input == ":h" || input == ":help") {
            defaultHelp(writer)
            writer.println("Please enter the amount of questions for the session.")
            return this
        } else if (input == ":q") {
            return parent
        }
        try {
            val count = input.toInt()
            val state = service.createSession(VocabularyService.SessionDescription(false, count))
            if (state.remainingQuestions == 0) {
                writer.println("No questions.")
                return parent
            }
            return QuestionHandler(parent, service)
        } catch (e: NumberFormatException) {
            writer.println("This is not a number.")
        }
        return this
    }

    override fun getLeftPrompt() = "question count"

    override fun getRightPrompt() = ""

    override fun handleInterrupt(writer: PrintWriter): TerminalHandler? = null
}