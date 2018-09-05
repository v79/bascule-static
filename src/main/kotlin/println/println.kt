package println

import org.fusesource.jansi.Ansi

fun info(string: String) {
	println(Ansi.ansi().fgBrightYellow().a(string).reset())
}

fun error(string: String) {
	println(Ansi.ansi().fgBrightRed().a(string).reset())
}