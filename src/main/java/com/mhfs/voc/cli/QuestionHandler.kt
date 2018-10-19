package com.mhfs.voc.cli

import com.mhfs.voc.VocabularyService
import java.io.PrintWriter

class QuestionHandler(private val parent: TerminalHandler, private val service: VocabularyService,
                      private val isActivation: Boolean = false) : TerminalHandler {

    private var isVerifying = false

    override fun handleInput(input: String, writer: PrintWriter): TerminalHandler? {
        if (input == ":q") {
            service.cancelSession()
            return parent
        } else if (input == ":h") {
            defaultHelp(writer)
            if (isVerifying) {
                writer.println("Please enter (y/n) weather the answer was correct.")
            } else {
                writer.println("Please write the answer to the question.")
            }
            return this
        }
        if (isVerifying) {
            val correct = input.toLowerCase().startsWith("y")
            writer.println("Marking answer accordingly.")
            service.correction(correct)
            isVerifying = false
        } else {
            val result = service.answer(input)
            when (result.result) {
                VocabularyService.ResultType.CORRECT -> writer.println("Correct.")
                VocabularyService.ResultType.WRONG -> writer.println("Wrong. Try again.")
                VocabularyService.ResultType.UNDETERMINED -> {
                    writer.println("Please verify.")
                    isVerifying = true
                }
            }
            writer.println("Solution: ${result.correctAnswer.text}")
        }
        return this
    }

    override fun getLeftPrompt(): String {
        return if (isVerifying) {
            "Correct? (y/n)"
        } else {
            val q = service.currentQuestion()
            val base = "${q.questionLanguage} -> ${q.targetLanguage}: ${q.question}"
            if (isActivation) {
                base + " -> ${service.getActivationSolution().text}"
            } else {
                base
            }
        }
    }

    override fun getRightPrompt() = "Remaining: ${service.getRemainingQuestionsCount()}"

    override fun handleInterrupt(writer: PrintWriter): TerminalHandler? = null

}