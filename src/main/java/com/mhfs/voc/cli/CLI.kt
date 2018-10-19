package com.mhfs.voc.cli

import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder

class CLI(private var handler: TerminalHandler) {

    private val terminal = TerminalBuilder.terminal()
    private val reader = LineReaderBuilder.builder().terminal(terminal).build()

    fun start() {
        while (true) {
            try {
                val input = reader.readLine(handler.getLeftPrompt() + "> ",  handler.getRightPrompt(), null as Char?, null)
                val result = handler.handleInput(input, terminal.writer())
                if (result != null) {
                    handler = result
                } else {
                    System.exit(0)
                }
            } catch (uie: UserInterruptException) {
                val result = handler.handleInterrupt(terminal.writer())
                if (result == null) System.exit(0)
                else handler = result
            }
        }
    }
}