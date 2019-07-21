package println

import org.fusesource.jansi.Ansi

class ProgressBar(val label: String = "", var messageLine: String? = null, val animated: Boolean = true, val bar: Boolean = false, val asPercentage: Boolean = false, val max: Int = 100) {

	val animationChars = charArrayOf('|', '/', '-', '\\')

	fun progress(current: Int, message: String) {
		messageLine = message
		progress(current)
	}

	fun progress(current: Int) {

		val progressString = StringBuilder()
		if(asPercentage) {
			progressString.append("%")
		}
		if(animated) {
			progressString.append(" ${animationChars[current % 4]}")
		}
		if(bar) {
			progressString.append(" ")
			progressString.append("=".repeat(current))
		}

		print(Ansi.ansi().eraseLine().fgBrightCyan().a("$label: $current$progressString").cursorDownLine())
		if (messageLine != null) {
			print(Ansi.ansi().eraseLine().fgBrightMagenta().a(messageLine))
		}
		print(Ansi.ansi().cursorUpLine())

	}

	fun clear() {
		print(Ansi.ansi().eraseLine().reset().cursorDownLine())
	}

}
