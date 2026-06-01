package org.liamjd.bascule.pipeline

import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.data.MutableDataSet
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.model.Tag
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import org.liamjd.bascule.model.BasculePost
import java.io.File
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TaxonomyNavigationGeneratorTest {

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
          generators: [IndexPageGenerator, PostNavigationGenerator, TaxonomyNavigationGenerator]
    """.trimIndent()

    private val project: Project = Project(yamlConfig = yamlConfig)

    private val mockRenderer = mockk<TemplatePageRenderer>()
    private val mockFileHandler = mockk<FileHandler>()
    private val tagsFolder = File("site/tags")

    private fun emptyDocument(): Document = Parser.builder(MutableDataSet()).build().parse("")

    private fun tag(label: String, postCount: Int) = Tag(category = "tags", label = label, url = label, postCount = postCount)

    private fun post(
        title: String,
        layout: String = "post",
        tags: Set<Tag> = emptySet(),
        date: LocalDate = LocalDate.of(2023, 1, 1)
    ): BasculePost {
        val post = BasculePost(emptyDocument())
        post.title = title
        post.slug = title.lowercase().replace(" ", "-")
        post.layout = layout
        post.date = date
        post.tags = tags.toMutableSet()
        return post
    }

    /**
     * Two posts share the tag "kotlin"; one post additionally has the tag "solo".
     * "kotlin" therefore has 2 posts and should get its own paginated index;
     * "solo" only has 1 post and should not.
     */
    private fun twoTaggedPosts(): List<BasculePost> = listOf(
        post("First", tags = setOf(tag("kotlin", 2), tag("solo", 1))),
        post("Second", tags = setOf(tag("kotlin", 2)))
    )

    private fun stubDirectoriesAndRender() {
        every { mockFileHandler.createDirectory(any(), "tags") } returns tagsFolder
        every { mockFileHandler.createDirectory(any(), "kotlin") } returns File("site/tags/kotlin")
        every { mockRenderer.render(any(), any()) } returns "<html></html>"
        every { mockFileHandler.writeFile(any(), any(), any()) } just Runs
    }

    @Test
    fun `creates the tags output directory`() {
        val generator = TaxonomyNavigationGenerator(twoTaggedPosts(), numPosts = 2, postsPerPage = 10)
        stubDirectoriesAndRender()

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        verify(exactly = 1) { mockFileHandler.createDirectory(project.dirs.output.absolutePath, "tags") }
    }

    @Test
    fun `always writes a tag list page`() {
        val generator = TaxonomyNavigationGenerator(twoTaggedPosts(), numPosts = 2, postsPerPage = 10)
        stubDirectoriesAndRender()

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        verify(exactly = 1) { mockRenderer.render(any(), "taglist") }
        verify(exactly = 1) { mockFileHandler.writeFile(tagsFolder, "tags.html", "<html></html>") }
    }

    @Test
    fun `writes a per-tag index page for tags with more than one post`() {
        val generator = TaxonomyNavigationGenerator(twoTaggedPosts(), numPosts = 2, postsPerPage = 10)
        stubDirectoriesAndRender()

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        verify(exactly = 1) { mockFileHandler.createDirectory(tagsFolder.absolutePath, "kotlin") }
        verify(exactly = 1) { mockFileHandler.writeFile(File("site/tags/kotlin"), "kotlin1.html", "<html></html>") }
        verify { mockRenderer.render(any(), "tag") }
    }

    @Test
    fun `does not write a per-tag index page for tags with only one post`() {
        val generator = TaxonomyNavigationGenerator(twoTaggedPosts(), numPosts = 2, postsPerPage = 10)
        stubDirectoriesAndRender()

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        verify(exactly = 0) { mockFileHandler.createDirectory(any(), "solo") }
        verify(exactly = 0) { mockFileHandler.writeFile(any(), match { it.startsWith("solo") }, any()) }
    }

    @Test
    fun `tag list page only includes tags that have more than one post`() {
        val generator = TaxonomyNavigationGenerator(twoTaggedPosts(), numPosts = 2, postsPerPage = 10)

        val modelSlot = slot<Map<String, Any?>>()
        every { mockFileHandler.createDirectory(any(), "tags") } returns tagsFolder
        every { mockFileHandler.createDirectory(any(), "kotlin") } returns File("site/tags/kotlin")
        every { mockRenderer.render(any(), "tag") } returns "<html></html>"
        every { mockRenderer.render(capture(modelSlot), "taglist") } returns "<html></html>"
        every { mockFileHandler.writeFile(any(), any(), any()) } just Runs

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        @Suppress("UNCHECKED_CAST")
        val tags = modelSlot.captured["tags"] as List<Tag>
        assertEquals(1, tags.size)
        assertEquals("kotlin", tags.first().label)
        assertEquals("List of tags", modelSlot.captured["title"])
    }

    @Test
    fun `excludes non-post layouts when building tags`() {
        // a "page" layout carries a tag that no real post has; it must not produce a tag page
        val posts = listOf(
            post("First", tags = setOf(tag("kotlin", 2))),
            post("Second", tags = setOf(tag("kotlin", 2))),
            post("About", layout = "page", tags = setOf(tag("draftonly", 2)))
        )
        val generator = TaxonomyNavigationGenerator(posts, numPosts = 2, postsPerPage = 10)
        stubDirectoriesAndRender()

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        verify(exactly = 0) { mockFileHandler.createDirectory(any(), "draftonly") }
        verify(exactly = 0) { mockFileHandler.writeFile(any(), match { it.startsWith("draftonly") }, any()) }
    }

    @Test
    fun `writes nothing but the tag list page when no tag has multiple posts`() {
        val posts = listOf(
            post("First", tags = setOf(tag("kotlin", 1))),
            post("Second", tags = setOf(tag("scala", 1)))
        )
        val generator = TaxonomyNavigationGenerator(posts, numPosts = 2, postsPerPage = 10)
        stubDirectoriesAndRender()

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        // only the "tags.html" list page is written, no per-tag pages
        verify(exactly = 1) { mockFileHandler.writeFile(any(), any(), any()) }
        verify(exactly = 1) { mockFileHandler.writeFile(tagsFolder, "tags.html", any()) }
    }
}
