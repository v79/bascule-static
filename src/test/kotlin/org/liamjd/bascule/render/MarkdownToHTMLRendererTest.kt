package org.liamjd.bascule.render

import com.vladsch.flexmark.util.ast.Document
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import org.liamjd.bascule.model.BasculePost
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * Tests for [MarkdownToHTMLRenderer]. The file handler and template renderer are supplied as mocks
 * via the constructor, so the Flexmark markdown conversion and the render/write orchestration can be
 * verified without Koin or the filesystem.
 */
class MarkdownToHTMLRendererTest {

    private val yamlConfig = """
        siteName: Liam John Davison
        dateFormat: "dd/MM/yyyy"
        dateTimeFormat: HH:mm:ss dd/MM/yyyy
        author: Liam Davison
        theme: liamjd-theme
        postsPerPage: 10
        directories:
          source: sources
          output: site
          assets: assets
          templates: liamjd-theme/templates
          generators: [IndexPageGenerator]
    """.trimIndent()

    private val project = Project(yamlConfig = yamlConfig)
    private val mockFileHandler = mockk<BasculeFileHandler>()
    private val mockRenderer = mockk<TemplatePageRenderer>()
    private val renderer = MarkdownToHTMLRenderer(project, mockFileHandler, mockRenderer)

    private fun parse(markdown: String): Document = renderer.mdParser.parse(markdown)

    @Test
    fun `renderMarkdown converts a parsed document to HTML`() {
        val html = renderer.renderMarkdown(parse("# Title\n\nSome **bold** text."))

        assertContains(html, "<h1")
        assertContains(html, "Title")
        assertContains(html, "<strong>bold</strong>")
    }

    @Test
    fun `renderHTML applies the post's layout template and writes the output file`() {
        val post = BasculePost(parse("Some **bold** body")).apply {
            sourceFileName = "my-post.md"
            title = "My Post"
            slug = "my-post"
            layout = "post"
            url = "my-post.html"
            destinationFolder = File("site/my-post")
        }

        every { mockRenderer.render(any(), "post") } returns "<html>FINAL</html>"
        every { mockFileHandler.createDirectories(any<File>()) } returns true
        every { mockFileHandler.writeFile(any(), any(), any()) } just Runs

        renderer.renderHTML(post, itemCount = 1)

        // the layout named in the post drives the template selection, and the rendered HTML is written
        // to the output directory under the post's url
        verify { mockRenderer.render(any(), "post") }
        verify { mockFileHandler.createDirectories(File("site/my-post")) }
        verify { mockFileHandler.writeFile(project.dirs.output.absoluteFile, "my-post.html", "<html>FINAL</html>") }
    }

    @Test
    fun `renderHTML puts the rendered markdown into the model and onto the post`() {
        val post = BasculePost(parse("Some **bold** body")).apply {
            sourceFileName = "my-post.md"
            title = "My Post"
            slug = "my-post"
            layout = "post"
            url = "my-post.html"
            destinationFolder = File("site/my-post")
        }

        val modelSlot = slot<Map<String, Any?>>()
        every { mockRenderer.render(capture(modelSlot), "post") } returns "<html>FINAL</html>"
        every { mockFileHandler.createDirectories(any<File>()) } returns true
        every { mockFileHandler.writeFile(any(), any(), any()) } just Runs

        renderer.renderHTML(post, itemCount = 1)

        val model = modelSlot.captured
        val renderedMarkdown = model["content"] as String
        assertContains(renderedMarkdown, "<strong>bold</strong>")
        // the post's content is updated with the same rendered markdown
        assertEquals(renderedMarkdown, post.content)
        // the post's own fields (via toModel) and the current page marker are present in the model
        assertEquals("My Post", model["title"])
        assertEquals("my-post", model["\$currentPage"])
    }
}
