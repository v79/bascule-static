package org.liamjd.bascule.render

import com.github.jknack.handlebars.Handlebars
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exercises [LocalDateFormatter] through a real Handlebars instance, since the helper's behaviour
 * depends on the positional params / hash supplied by the template engine.
 */
class LocalDateFormatterTest {

	private val handlebars = Handlebars().apply {
		registerHelper("localDate", LocalDateFormatter())
	}

	private val date = LocalDate.of(2026, 1, 9)

	private fun render(template: String, context: Any?): String =
		handlebars.compileInline(template).apply(context)

	@Test
	fun `uses the default input format when none is supplied`() {
		assertEquals("09/01/2026", render("{{localDate this}}", date))
	}

	@Test
	fun `formats a date using a custom pattern from the format hash`() {
		assertEquals("2026-01-09", render("{{localDate this format=\"yyyy-MM-dd\"}}", date))
	}

	@Test
	fun `formats a date using a positional custom pattern`() {
		assertEquals("09-01-2026", render("{{localDate this \"dd-MM-yyyy\"}}", date))
	}

	@Test
	fun `formats a date using the named ISO_DATE format`() {
		assertEquals("2026-01-09", render("{{localDate this \"ISO_DATE\"}}", date))
	}

	@Test
	fun `falls back to ISO_LOCAL_DATE when the pattern is invalid`() {
		// A lone single-quote is an unterminated literal, which DateTimeFormatter rejects.
		assertEquals("2026-01-09", render("{{localDate this \"'\"}}", date))
	}
}
