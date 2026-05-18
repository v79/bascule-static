package org.liamjd.bascule.model

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.data.MutableDataSet
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.liamjd.bascule.lib.model.Project
import java.io.File
import kotlin.test.*


class BasculePostTest {

    private val mockYamlVisitor: AbstractYamlFrontMatterVisitor = mockk()
    private val mockFile: File = File("src/test/resources/NoYaml.md")

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
    private val koinModule = module {
        factory { mockYamlVisitor }
    }

    private val frontispiece: MutableMap<String, List<String>> = mutableMapOf(
        "title" to listOf("Test Post"),
        "layout" to listOf("post"),
        "author" to listOf("William Shakespeare"),
        "slug" to listOf("test-post")
    )

    @BeforeEach
    fun setup() {
        every { mockYamlVisitor.visit(any<Document>()) } just Runs

        startKoin {
            modules(koinModule)
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `can create a BasculePost from a Document`() {
        // setup
        val markdown = """
            ---
            title: "Test Post"
            layout: post
            author: "William Shakespeare"
            slug: "test-post"
            ---
            Some content
            """.trimIndent()

        every { mockYamlVisitor.data } returns frontispiece.toMap()
        // execute
        val document = parseMarkdown(markdown)
        val post = BasculePost.createPostFromYaml(mockFile, document, project)

        // verify
        assertNotNull(post)
        when (post) {
            is PostGenError -> {
                throw AssertionError("Expected a BasculePost, but got a PostGenError: ${post.errorMessage}")
            }

            is BasculePost -> {
                assertEquals("Test Post", post.title)
                assertEquals("post", post.layout)
                assertEquals("William Shakespeare", post.author)
                assertEquals("test-post", post.slug) // Slug should be generated from title
                assertNotNull(post.document)
            }
        }
    }

    @Test
    fun `builds a post from file details if no YAML is supplied`() {
        // setup
        val markdown = """
            Some content without YAML front matter
            """.trimIndent()
        val document = parseMarkdown(markdown)
        every { mockYamlVisitor.data } returns emptyMap() // Simulate no YAML frontispiece

        // execute
        val post = BasculePost.createPostFromYaml(mockFile, document, project)

        // verify
        assertNotNull(post)
        assertIs<BasculePost>(post)
        assertEquals("NoYaml", post.title) // Default title if no YAML is present
        assertEquals("post", post.layout) // Default layout if not specified
        assertEquals("noyaml", post.slug) // Default slug if not specified
    }

    @Test
    fun `throws error if required YAML field is missing`() {
        // setup
        val markdown = """
            ---
            layout: post
            ---
            Some content
            """.trimIndent()
        val document = parseMarkdown(markdown)
        every { mockYamlVisitor.data } returns mapOf("layout" to listOf("post")) // Missing 'title'

        // execute
        val post = BasculePost.createPostFromYaml(mockFile, document, project)

        // verify
        assertIs<PostGenError>(post)
        assertEquals("Required field 'title' not found", post.errorMessage)
    }

    @Test
    fun `throw error if required YAML field exists but has no value`() {
        // setup
        val markdown = """
            ---
            title: 
            layout: post
            ---
            Some content
            """.trimIndent()
        val document = parseMarkdown(markdown)
        every { mockYamlVisitor.data } returns mapOf("title" to listOf(""), "layout" to listOf("post")) // Empty title

        // execute
        val post = BasculePost.createPostFromYaml(mockFile, document, project)

        // verify
        assertIs<PostGenError>(post)
        assertEquals("Missing required field 'title' in source file 'NoYaml.md'", post.errorMessage)
    }

    @Test
    fun `throw error if YAML field is not allowed to have multiple values`() {
        // setup
        val markdown = """
            ---
            title: Two Dates
            layout: post
            date: [2023-10-01, 2023-10-02]
            ---
            Some content
            """.trimIndent()
        val document = parseMarkdown(markdown)
        every { mockYamlVisitor.data } returns mapOf(
            "title" to listOf("Two Dates"),
            "layout" to listOf("post"),
            "date" to listOf("2023-10-01", "2023-10-02")
        ) // Multiple dates

        // execute
        val post = BasculePost.createPostFromYaml(mockFile, document, project)

        // verify
        assertIs<PostGenError>(post)
        assertEquals(
            "Field 'date' is only allowed a single value; found '2' in source file 'NoYaml.md'",
            post.errorMessage
        )

    }

    @Test
    fun `can parse markdown date with default dd MM yyyy format`() {
        // setup
        val markdown = """
            ---
            title: "Test Post with Date"
            layout: post
            date: "01/10/2023"
            ---
            Some content
            """.trimIndent()
        every { mockYamlVisitor.data } returns mapOf(
            "title" to listOf("Test Post with Date"),
            "layout" to listOf("post"),
            "date" to listOf("01/10/2023")
        )

        // execute
        val document = parseMarkdown(markdown)
        val post = BasculePost.createPostFromYaml(mockFile, document, project)

        // verify
        assertNotNull(post)
        assertIs<BasculePost>(post)
        assertEquals("Test Post with Date", post.title)
        assertEquals("post", post.layout)
        assertEquals(1, post.date.dayOfMonth) // Assuming date is parsed correctly
        assertEquals(10, post.date.monthValue)
        assertEquals(2023, post.date.year)
    }

    @Test
    fun `extracts a list of tags from a comma separated array`() {
        // setup
        val markdown = """
            ---
            title: "Test Post with Tags"
            layout: post
            tags: [Kotlin, testing, Bascule, Software Development]
            ---
            Some content
            """.trimIndent()
        every { mockYamlVisitor.data } returns mapOf(
            "title" to listOf("Test Post with Tags"),
            "layout" to listOf("post"),
            "tags" to listOf("[Kotlin, testing, Bascule, Software Development]")
        )

        // execute
        val document = parseMarkdown(markdown)
        val post = BasculePost.createPostFromYaml(mockFile, document, project)

        // verify
        assertNotNull(post)
        assertIs<BasculePost>(post)
        assertEquals(4, post.tags.size) // Should have 4 tags
    }

    @Test
    fun `get summary doesn't actually work`() {
        // setup
        val markdown = """
            ---
            title: "Test Post with Summary"
            layout: post
            ---
            This is a test post content that should be summarized.
            """.trimIndent()
        every { mockYamlVisitor.data } returns mapOf(
            "title" to listOf("Test Post with Summary"),
            "layout" to listOf("post")
        )

        // execute
        val document = parseMarkdown(markdown)
        val post = BasculePost.createPostFromYaml(mockFile, document, project)

        // verify
        assertNotNull(post)
        assertIs<BasculePost>(post)
        assertTrue(post.getSummary(10).length == 3) // Summary includes "..."
        assertEquals("...", post.getSummary(10))
    }

    @Test
    fun `unknown yaml items are added as post attributes`() {
        // setup
        val markdown = """
            ---
            title: "Test Post with Unknown Attributes"
            layout: post
            unknownField: "This is an unknown field"
            anotherUnknown: "Another value"
            ---
            Some content
            """.trimIndent()
        every { mockYamlVisitor.data } returns mapOf(
            "title" to listOf("Test Post with Unknown Attributes"),
            "layout" to listOf("post"),
            "unknownField" to listOf("This is an unknown field"),
            "anotherUnknown" to listOf("Another value")
        )

        // execute
        val document = parseMarkdown(markdown)
        val post = BasculePost.createPostFromYaml(mockFile, document, project)

        // verify
        assertNotNull(post)
        assertIs<BasculePost>(post)
        assertEquals(2, post.attributes.size)
        assertEquals("This is an unknown field", post.attributes["unknownField"])
        assertEquals("Another value", post.attributes["anotherUnknown"])
    }

    // Mocking the Document AST object is complex, so we will parse a simple markdown string instead
    private fun parseMarkdown(markdown: String): Document {
        val mdParser: Parser = Parser.builder(MutableDataSet()).build()
        return mdParser.parse(markdown)
    }

}