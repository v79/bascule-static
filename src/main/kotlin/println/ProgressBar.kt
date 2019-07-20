package println

import mu.KotlinLogging
import org.fusesource.jansi.Ansi

class ProgressBar(val label: String = "", var messageLine: String? = null, val animated: Boolean = true, val bar: Boolean = false, val asPercentage: Boolean = false, val max: Int = 100) {

	private val logger = KotlinLogging.logger {}

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

		Thread.sleep(500)

		print(Ansi.ansi().eraseLine().fgBrightCyan().a("$label: $current$progressString").cursorDownLine())
		logger.debug { "$label: $current$progressString" }
		logger.info { "$label: $current$progressString" }
		logger.warn { "$label: $current$progressString" }
		logger.error { "$label: $current$progressString" }
		if (messageLine != null) {
			print(Ansi.ansi().eraseLine().fgBrightMagenta().a(messageLine))
		}
		print(Ansi.ansi().cursorUpLine())

	}

	fun clear() {
		print(Ansi.ansi().eraseLine().reset().cursorDownLine())
	}

}
