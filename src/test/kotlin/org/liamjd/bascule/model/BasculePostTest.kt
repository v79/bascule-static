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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class BasculePostTest {

    private val mockYamlVisitor: AbstractYamlFrontMatterVisitor = mockk()
    private val mockFile: File = File("src/test/resources/posts/Simple File (1).md")

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
        "layout" to listOf("post")
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
        every { mockYamlVisitor.data } returns frontispiece.toMap()
        val markdown = """
            ---
            title: "Test Post"
            layout: post
            ---
            Some content
            """.trimIndent()
        val document = parseMarkdown(markdown)

        // execute
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
                assertNotNull(post.document)
            }
        }
    }


    // Mocking the Document AST object is complex, so we will parse a simple markdown string instead
    private fun parseMarkdown(markdown: String): Document {
        val mdParser: Parser = Parser.builder(MutableDataSet()).build()
        return mdParser.parse(markdown)
    }

}