package org.liamjd.bascule.render

import com.github.jknack.handlebars.Handlebars
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the custom `#paginate` block helper. It iterates a list of page tokens (page numbers plus
 * the special markers "*" for the current page and "." for an ellipsis), exposing per-token loop
 * variables. Positional params are the current page and the total page count.
 */
class PaginateTest {

	private val handlebars = Handlebars().apply {
		registerHelper("paginate", Paginate())
	}

	private fun render(template: String, context: Any?): String =
		handlebars.compileInline(template).apply(context)

	@Test
	fun `emits the page number and flags first and last tokens`() {
		val result = render(
			"{{#paginate this 1 3}}{{@page}}:{{@first}}{{@last}};{{/paginate}}",
			listOf("1", "2", "3")
		)
		assertEquals("1:first;2:;3:last;", result)
	}

	@Test
	fun `flags the current-page and ellipsis markers`() {
		val result = render(
			"{{#paginate this 1 5}}{{@current}}{{@ellipsis}}|{{/paginate}}",
			listOf("1", "*", ".")
		)
		assertEquals("|current|ellipsis|", result)
	}

	@Test
	fun `leaves the page variable blank for non-numeric tokens`() {
		val result = render(
			"{{#paginate this 1 5}}[{{@page}}]{{/paginate}}",
			listOf("1", "*", ".")
		)
		assertEquals("[1][][]", result)
	}

	@Test
	fun `returns an explanatory message when the context is not iterable`() {
		val result = render("{{#paginate this 1 3}}body{{/paginate}}", 42)
		assertTrue(result.contains("not iterable"), "expected a not-iterable message but got: $result")
	}
}
