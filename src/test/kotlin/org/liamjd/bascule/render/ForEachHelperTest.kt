package org.liamjd.bascule.render

import com.github.jknack.handlebars.Handlebars
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exercises the custom `#forEach` block helper through a real Handlebars instance. `#forEach` behaves
 * like `#each` but accepts an optional positional limit and exposes extra loop variables.
 */
class ForEachHelperTest {

	private val handlebars = Handlebars().apply {
		registerHelper("forEach", ForEachHelper())
	}

	private fun render(template: String, context: Any?): String =
		handlebars.compileInline(template).apply(context)

	@Test
	fun `iterates over every element when no limit is given`() {
		assertEquals("a,b,c,", render("{{#forEach this}}{{this}},{{/forEach}}", listOf("a", "b", "c")))
	}

	@Test
	fun `honours a positional limit`() {
		assertEquals("a,b,", render("{{#forEach this \"2\"}}{{this}},{{/forEach}}", listOf("a", "b", "c")))
	}

	@Test
	fun `renders the inverse block for an empty collection`() {
		assertEquals("none", render("{{#forEach this}}{{this}}{{else}}none{{/forEach}}", emptyList<String>()))
	}

	@Test
	fun `exposes a one-based index via index_1`() {
		assertEquals("1.2.3.", render("{{#forEach this}}{{@index_1}}.{{/forEach}}", listOf("a", "b", "c")))
	}

	@Test
	fun `flags the first and last elements`() {
		assertEquals(
			"first;last;",
			render("{{#forEach this}}{{@first}}{{@last}};{{/forEach}}", listOf("a", "b"))
		)
	}

	@Test
	fun `alternates odd and even markers`() {
		assertEquals(
			"even,odd,even,",
			render("{{#forEach this}}{{@even}}{{@odd}},{{/forEach}}", listOf("a", "b", "c"))
		)
	}
}
