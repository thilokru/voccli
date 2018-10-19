package com.mhfs.voc.cli

import java.io.PrintWriter

interface TerminalHandler {

    fun handleInput(input: String, writer: PrintWriter): TerminalHandler?

    fun getLeftPrompt(): String

    fun getRightPrompt(): String

    /**
     * Currently not working.
     */
    fun handleInterrupt(writer: PrintWriter): TerminalHandler?

    fun defaultHelp(writer: PrintWriter) {
        writer.println("To obtain help at any moment, write ':h' (without the quotation marks)")
        writer.println("To return to the previous layer or quit the application, type ':q'")
    }
}