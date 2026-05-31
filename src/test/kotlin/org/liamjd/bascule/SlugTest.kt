package org.liamjd.bascule

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the [String.slug] extension in Constants.kt. It lowercases the string and replaces every
 * character outside [a-zA-Z0-9-] with a single dash.
 */
class SlugTest {

	@Test
	fun `lowercases the input`() {
		assertEquals("hello", "HELLO".slug())
	}

	@Test
	fun `replaces spaces with dashes`() {
		assertEquals("hello-world", "Hello World".slug())
	}

	@Test
	fun `preserves digits`() {
		assertEquals("post-123", "Post 123".slug())
	}

	@Test
	fun `keeps existing dashes untouched`() {
		assertEquals("already-slugged", "already-slugged".slug())
	}

	@Test
	fun `replaces each punctuation character with its own dash`() {
		// "foo & bar!" -> space, &, space and ! each become a dash (no collapsing)
		assertEquals("foo---bar-", "foo & bar!".slug())
	}

	@Test
	fun `does NOT collapse consecutive separators`() {
		// documented quirk: two spaces produce two dashes
		assertEquals("a--b", "a  b".slug())
	}

	@Test
	fun `leaves an already clean string unchanged`() {
		assertEquals("simple", "simple".slug())
	}
}
