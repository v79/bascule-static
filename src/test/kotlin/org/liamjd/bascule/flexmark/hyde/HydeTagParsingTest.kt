package org.liamjd.bascule.flexmark.hyde

import com.vladsch.flexmark.ast.util.Parsing
import com.vladsch.flexmark.util.data.MutableDataSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the regular expressions in [HydeTagParsing] that recognise the `{§ tag params §}`
 * macro syntax.
 */
class HydeTagParsingTest {

    private val parsing = HydeTagParsing(Parsing(MutableDataSet()))

    @Test
    fun `MACRO_OPEN matches a standalone tag and captures the tag name`() {
        val matcher = parsing.MACRO_OPEN.matcher("{§ include snippet.html §}")
        assertTrue(matcher.find(), "expected the macro-open pattern to match a standalone tag")
        assertEquals("include", matcher.group(1))
    }

    @Test
    fun `MACRO_OPEN matches a tag with no parameters`() {
        val matcher = parsing.MACRO_OPEN.matcher("{§ include §}")
        assertTrue(matcher.find())
        assertEquals("include", matcher.group(1))
    }

    @Test
    fun `MACRO_OPEN does not match ordinary prose`() {
        val matcher = parsing.MACRO_OPEN.matcher("this is just a normal paragraph of text")
        assertFalse(matcher.find())
    }

    @Test
    fun `MACRO_OPEN does not match a tag with leading text on the line`() {
        // MACRO_OPEN is anchored to the whole line, so a tag preceded by prose should not match
        val matcher = parsing.MACRO_OPEN.matcher("intro {§ include snippet.html §}")
        assertFalse(matcher.find())
    }

    @Test
    fun `MACRO_TAG finds a tag embedded within a line`() {
        // MACRO_TAG is unanchored, so it locates the tag even mid-line
        val matcher = parsing.MACRO_TAG.matcher("intro {§ include snippet.html §} outro")
        assertTrue(matcher.find())
        assertEquals("include", matcher.group(1))
    }
}
