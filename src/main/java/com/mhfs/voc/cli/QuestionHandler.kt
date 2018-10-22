package com.mhfs.voc.cli

import com.mhfs.voc.VocabularyService
import com.mhfs.voc.VocabularyService.*
import java.io.PrintWriter

class QuestionHandler(private val parent: TerminalHandler, private val service: VocabularyService) : TerminalHandler {

    private var state = service.getState()

    override fun handleInput(input: String, writer: PrintWriter): TerminalHandler? {
        if (input == ":q") {
            service.cancelSession()
            return parent
        } else if (input == ":h") {
            defaultHelp(writer)
            if (isVerifying()) {
                writer.println("Please enter (y/n) weather the answer was correct.")
            } else {
                writer.println("Please write the answer to the question.")
            }
            return this
        }

        if (isVerifying()) {
            val correct = input.toLowerCase().startsWith("y")
            writer.println("Marking answer accordingly.")
            state = service.correction(correct)
        } else {
            state = service.answer(input)
            when (state.lastResult!!.type) {
                ResultType.CORRECT -> writer.println("Correct.")
                ResultType.WRONG -> writer.println("Wrong. Try again.")
                ResultType.UNDETERMINED -> {
                    writer.println("Please verify.")
                }
            }
            writer.println("Solution: ${state.lastResult!!.solution}")
        }
        if (state.remainingQuestions == 0) {
            writer.println("Session completed.")
            return parent
        }
        return this
    }

    override fun getLeftPrompt(): String {
        return if (isVerifying()) {
            "Correct? (y/n)"
        } else if(state.currentQuestion != null) {
            val q = state.currentQuestion!!
            val base = "${q.questionLanguage} -> ${q.targetLanguage}: ${q.question}"
            if (q.solution != null) {
                base + " -> ${q.solution}"
            } else {
                base
            }
        } else ""
    }

    private fun isVerifying(): Boolean {
        return state.lastResult != null && state.lastResult!!.type == ResultType.UNDETERMINED
    }

    override fun getRightPrompt() = "Remaining: ${state.remainingQuestions}"

    override fun handleInterrupt(writer: PrintWriter): TerminalHandler? = null

}