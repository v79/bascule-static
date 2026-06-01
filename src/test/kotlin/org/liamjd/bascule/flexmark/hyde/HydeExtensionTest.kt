package org.liamjd.bascule.flexmark.hyde

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.misc.Extension
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * End-to-end tests for the Hyde transclusion extension, driving the real Flexmark parser and HTML
 * renderer exactly as [org.liamjd.bascule.generator.Generator] wires them up. Transclusion sources
 * are read from src/test/resources/hyde.
 */
class HydeExtensionTest {

    private val sourceFolder = "src/test/resources/hyde"
    private val includedContent = "Hello from the included snippet"

    private fun renderMarkdown(markdown: String): String {
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf<Extension>(HydeExtension.create()))
        options.set(HydeExtension.SOURCE_FOLDER, sourceFolder)
        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()
        return renderer.render(parser.parse(markdown))
    }

    @Test
    fun `include transcludes the content of an existing file`() {
        val html = renderMarkdown("{§ include snippet.html §}")
        assertContains(html, includedContent)
    }

    @Test
    fun `a missing include file renders a not-found message`() {
        val html = renderMarkdown("{§ include does-not-exist.html §}")
        assertContains(html, "does-not-exist.html not found")
        assertContains(html, "<em>")
        assertFalse(html.contains(includedContent))
    }

    @Test
    fun `an unknown tag name is left as literal text and not transcluded`() {
        // "wibble" is a valid tag name shape but the parser only acts on "include"
        val html = renderMarkdown("{§ wibble snippet.html §}")
        assertContains(html, "wibble")
        assertFalse(html.contains(includedContent), "unknown tag must not pull in the file content")
    }

    @Test
    fun `a tag embedded in a paragraph is not transcluded`() {
        // the tag is a lazy continuation of the preceding paragraph, so it is treated as plain text
        val html = renderMarkdown("Some preceding text\n{§ include snippet.html §}")
        assertFalse(html.contains(includedContent), "a tag inside a paragraph must not be transcluded")
        assertContains(html, "include snippet.html")
    }

    @Test
    fun `multiple includes are each transcluded`() {
        val html = renderMarkdown(
            """
            {§ include snippet.html §}

            {§ include snippet.html §}
            """.trimIndent()
        )
        val occurrences = Regex(Regex.escape(includedContent)).findAll(html).count()
        assertContains(html, includedContent)
        assert(occurrences == 2) { "expected the snippet to be included twice, found $occurrences" }
    }
}
