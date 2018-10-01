package org.liamjd.bascule.render

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Only handles LocalDate with no time component
 */
class LocalDateFormatter(val inputFormat: String) : Helper<Any> {

	private val ISO_DATE = "ISO_DATE"
	private val ISO_LOCAL_DATE = "ISO_LOCAL_DATE"
	private val ISO_ORDINAL_DATE = "ISO_ORDINAL_DATE"
	private val ISO_WEEK_DATE = "ISO_WEEK_DATE"

	override fun apply(value: Any?, options: Options): CharSequence {

		if (value != null) {
			val format = options.param(0, options.hash<Any>("format", inputFormat)) as String
			val outputFormatter = when (format) {
				ISO_DATE -> {
					DateTimeFormatter.ISO_DATE
				}
				ISO_LOCAL_DATE -> {
					DateTimeFormatter.ISO_LOCAL_DATE
				}
				ISO_ORDINAL_DATE -> {
					DateTimeFormatter.ISO_ORDINAL_DATE
				}
				ISO_WEEK_DATE -> {
					DateTimeFormatter.ISO_WEEK_DATE
				}
				else -> {
					try {
						DateTimeFormatter.ofPattern(format)
					}  catch (iae: IllegalArgumentException) {
						println("Could not parse localDate format string; reverting to ISO_LOCAL_DATE")
						DateTimeFormatter.ISO_LOCAL_DATE
					}
				}
			}
			val result =	outputFormatter.format(value as LocalDate)
			return result
		}
		return LocalDate.now().toString()
	}
}
