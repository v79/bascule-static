package println

import org.fusesource.jansi.Ansi

val debug = true

/**
 * Prints text in a nice yellow colour
 */
fun info(string: String) {
	println(Ansi.ansi().eraseLine().fgBrightYellow().a(string).reset())
}

/**
 * Prints text in a scary red colour
 */
fun error(string: String) {
	println(Ansi.ansi().fgBrightRed().a("ERROR: $string").reset())
}

fun debug(string: String) {
	if (debug) {
		println(Ansi.ansi().fgCyan().a("DEBUG: $string").reset())
	}
}
