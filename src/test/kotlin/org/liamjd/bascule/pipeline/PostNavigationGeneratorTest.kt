package org.liamjd.bascule.pipeline

import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.data.MutableDataSet
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import org.liamjd.bascule.model.BasculePost
import java.io.File
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostNavigationGeneratorTest {

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
    private val postsFolder = File("site/posts")
    private val blogsFolder = File("site/blogs")

    private fun emptyDocument(): Document = Parser.builder(MutableDataSet()).build().parse("")

    private fun post(title: String, layout: String = "post", date: LocalDate = LocalDate.of(2023, 1, 1)): BasculePost {
        val post = BasculePost(emptyDocument())
        post.title = title
        post.slug = title.lowercase().replace(" ", "-")
        post.layout = layout
        post.date = date
        return post
    }

    @Test
    fun `writes one list page per group of posts`() {
        // setup: 3 posts with postsPerPage = 2 should produce 2 pages
        val posts: List<Post> = listOf(
            post("First", date = LocalDate.of(2023, 1, 1)),
            post("Second", date = LocalDate.of(2023, 2, 1)),
            post("Third", date = LocalDate.of(2023, 3, 1))
        )
        val generator = PostNavigationGenerator(posts, numPosts = 3, postsPerPage = 2)

        every { mockFileHandler.createDirectory(any(), "posts") } returns postsFolder
        every { mockRenderer.render(any(), any()) } returns "<html></html>"
        every { mockFileHandler.writeFile(any(), any(), any()) } just Runs

        // execute
        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        // verify two list pages are written, named by page number
        verify(exactly = 1) { mockFileHandler.writeFile(postsFolder, "list1.html", "<html></html>") }
        verify(exactly = 1) { mockFileHandler.writeFile(postsFolder, "list2.html", "<html></html>") }
        verify(exactly = 2) { mockFileHandler.writeFile(any(), any(), any()) }
    }

    @Test
    fun `creates the posts output directory`() {
        val generator = PostNavigationGenerator(listOf(post("Only")), numPosts = 1, postsPerPage = 10)

        every { mockFileHandler.createDirectory(any(), "posts") } returns postsFolder
        every { mockRenderer.render(any(), any()) } returns "<html></html>"
        every { mockFileHandler.writeFile(any(), any(), any()) } just Runs

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        verify(exactly = 1) { mockFileHandler.createDirectory(project.dirs.output.absolutePath, "posts") }
    }

    @Test
    fun `renders using the list template`() {
        val generator = PostNavigationGenerator(listOf(post("Only")), numPosts = 1, postsPerPage = 10)

        every { mockFileHandler.createDirectory(any(), "posts") } returns postsFolder
        every { mockRenderer.render(any(), any()) } returns "<html></html>"
        every { mockFileHandler.writeFile(any(), any(), any()) } just Runs

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        verify { mockRenderer.render(any(), "list") }
    }

    @Test
    fun `excludes posts that do not have the post layout`() {
        // setup: one real post and one "page" layout that must be filtered out
        val posts: List<Post> = listOf(
            post("A blog post", layout = "post"),
            post("An about page", layout = "page")
        )
        val generator = PostNavigationGenerator(posts, numPosts = 1, postsPerPage = 10)

        val modelSlot = slot<Map<String, Any?>>()
        every { mockFileHandler.createDirectory(any(), "posts") } returns postsFolder
        every { mockRenderer.render(capture(modelSlot), "list") } returns "<html></html>"
        every { mockFileHandler.writeFile(any(), any(), any()) } just Runs

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        @Suppress("UNCHECKED_CAST")
        val renderedPosts = modelSlot.captured["posts"] as List<Post>
        assertEquals(1, renderedPosts.size)
        assertEquals("A blog post", renderedPosts.first().title)
    }

    @Test
    fun `writes no list pages when there are no posts`() {
        val generator = PostNavigationGenerator(emptyList(), numPosts = 0, postsPerPage = 10)

        every { mockFileHandler.createDirectory(any(), "posts") } returns postsFolder
        every { mockRenderer.render(any(), any()) } returns "<html></html>"
        every { mockFileHandler.writeFile(any(), any(), any()) } just Runs

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        verify(exactly = 0) { mockFileHandler.writeFile(any(), any(), any()) }
    }

    @Test
    fun `orders posts on a page from newest to oldest`() {
        val posts: List<Post> = listOf(
            post("Oldest", date = LocalDate.of(2023, 1, 1)),
            post("Newest", date = LocalDate.of(2023, 3, 1)),
            post("Middle", date = LocalDate.of(2023, 2, 1))
        )
        val generator = PostNavigationGenerator(posts, numPosts = 3, postsPerPage = 10)

        val modelSlot = slot<Map<String, Any?>>()
        every { mockFileHandler.createDirectory(any(), "posts") } returns postsFolder
        every { mockRenderer.render(capture(modelSlot), "list") } returns "<html></html>"
        every { mockFileHandler.writeFile(any(), any(), any()) } just Runs

        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        @Suppress("UNCHECKED_CAST")
        val renderedPosts = modelSlot.captured["posts"] as List<Post>
        assertEquals(listOf("Newest", "Middle", "Oldest"), renderedPosts.map { it.title })
        assertTrue(renderedPosts.first().date.isAfter(renderedPosts.last().date))
    }

    /**
     * Regression test for the indexing quirk where `withIndex()` ran *before* the non-post layouts
     * were filtered out: removing an interleaved non-post left a gap in the index sequence, and the
     * subsequent `groupBy { index / postsPerPage }` scattered posts across extra, under-filled pages
     * while the model's `totalPages` reported a different figure.
     *
     * Here: 3 posts interleaved with 2 non-posts at postsPerPage = 2. The non-posts must be ignored
     * entirely, leaving 2 full-then-remainder pages (2 posts + 1 post), newest first, with a
     * `totalPages` that matches the number of files written.
     */
    @Test
    fun `interleaved non-post layouts are ignored and pages fill correctly`() {
        // constructor order [post, page, post, page, post]
        val posts: List<Post> = listOf(
            post("Oldest post", layout = "post", date = LocalDate.of(2023, 1, 1)),
            post("A page", layout = "page", date = LocalDate.of(2023, 1, 15)),
            post("Middle post", layout = "post", date = LocalDate.of(2023, 2, 1)),
            post("Another page", layout = "page", date = LocalDate.of(2023, 2, 15)),
            post("Newest post", layout = "post", date = LocalDate.of(2023, 3, 1))
        )
        val generator = PostNavigationGenerator(posts, numPosts = 3, postsPerPage = 2)

        val models = mutableListOf<Map<String, Any?>>()
        val fileNames = mutableListOf<String>()
        every { mockFileHandler.createDirectory(any(), "posts") } returns postsFolder
        every { mockRenderer.render(capture(models), "list") } returns "<html></html>"
        every { mockFileHandler.writeFile(postsFolder, capture(fileNames), any()) } just Runs

        // execute
        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        // exactly 2 pages, named sequentially
        assertEquals(listOf("list1.html", "list2.html"), fileNames)

        @Suppress("UNCHECKED_CAST")
        val page1 = models[0]["posts"] as List<Post>

        @Suppress("UNCHECKED_CAST")
        val page2 = models[1]["posts"] as List<Post>

        // first page is full (postsPerPage = 2), second page holds the remainder
        assertEquals(listOf("Newest post", "Middle post"), page1.map { it.title })
        assertEquals(listOf("Oldest post"), page2.map { it.title })

        // no non-post ever leaks into a page
        assertTrue(
            (page1 + page2).none { it.layout == "page" },
            "non-post layouts must not appear in any list page"
        )

        // the model's page count now matches the number of files written
        assertEquals(2, models[0]["totalPages"])
        assertEquals(2, models[1]["totalPages"])
    }

    @Test
    fun `runs post generator for each layout defined in postLayouts`() {
        project.postLayouts = setOf("post", "blog")
        val posts: List<Post> = listOf(
            post("First", layout = "post"),
            post("Second", layout = "blog")
        )
        val generator = PostNavigationGenerator(posts, numPosts = 2, postsPerPage = 10)

        val models = mutableListOf<Map<String, Any?>>()
        val fileNames = mutableListOf<String>()
        every { mockFileHandler.createDirectory(any(), "posts") } returns postsFolder
        every { mockFileHandler.createDirectory(any(), "blogs") } returns blogsFolder
        every { mockRenderer.render(capture(models), "list") } returns "<html></html>"
        every { mockFileHandler.writeFile(postsFolder, capture(fileNames), any()) } just Runs
        every { mockFileHandler.writeFile(blogsFolder, capture(fileNames), any()) } just Runs

        // execute
        runBlocking { generator.process(project, mockRenderer, mockFileHandler, clean = true) }

        val postModel = models.first { it["layout"] == "post" }
        val blogModel = models.first { it["layout"] == "blog" }

        @Suppress("UNCHECKED_CAST")
        val renderedPosts = postModel["posts"] as List<Post>

        @Suppress("UNCHECKED_CAST")
        val renderedBlogs = blogModel["posts"] as List<Post>

        assertEquals(1, renderedPosts.size)
        assertEquals(renderedPosts.first().title, "First")
        assertEquals(1, renderedBlogs.size)
        assertEquals(renderedBlogs.first().title, "Second")
        assertEquals(2, fileNames.size)
        verify(exactly = 1) { mockFileHandler.writeFile(postsFolder, "list1.html", any()) }
        verify(exactly = 1) { mockFileHandler.writeFile(blogsFolder, "list1.html", any()) }

    }
}
