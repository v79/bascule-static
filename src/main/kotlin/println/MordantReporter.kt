package println

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal

/**
 * A reporter that uses Mordant for coloured and animated terminal output.
 */
class MordantReporter : Reporter {

    override var verbose: Boolean = false
    private val terminal = Terminal()
    private val animationChars = charArrayOf('|', '/', '-', '\\')

    override fun info(msg: String) {
        terminal.println(TextColors.brightYellow(msg))
    }

    override fun error(msg: String) {
        terminal.println(TextColors.brightRed("ERROR: $msg"))
    }

    override fun warn(msg: String) {
        terminal.println(TextColors.brightMagenta("WARNING: $msg"))
    }

    override fun debug(msg: String) {
        if (verbose) terminal.println(TextColors.cyan("DEBUG: $msg"))
    }

    override fun progress(label: String, current: Int, message: String?) {
        val spinner = animationChars[current % animationChars.size]
        terminal.cursor.move {
            startOfLine()
            clearLine()
        }
        terminal.print(TextColors.brightCyan("$label: $current $spinner"))
        if (message != null) {
            // startOfLine() before down(1) ensures we arrive at col 0 of the next line,
            // because Mordant's down(1) preserves the current column unlike Jansi's cursorDownLine
            terminal.cursor.move { startOfLine(); down(1); clearLine() }
            terminal.print(TextColors.brightMagenta(message))
            terminal.cursor.move { up(1); startOfLine() }
        }
    }

    override fun clearProgress() {
        terminal.cursor.move {
            startOfLine()
            clearLine()
            down(1)
            clearLine()
            up(1)
        }
    }
}
