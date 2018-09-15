package org.liamjd.bascule.render

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateFormatter(val inputFormat: String) : Helper<Any> {

	override fun apply(value: Any?, options: Options): CharSequence {

		if (value != null) {
			val date = value as LocalDate
			val outputFormatter: DateTimeFormatter
			val pattern = options.param(0, options.hash<Any>("format", inputFormat)) as String
			outputFormatter = DateTimeFormatter.ofPattern(pattern)
			return outputFormatter.format(value)
		}
		return LocalDate.now().toString()
	}
}
