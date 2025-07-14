package org.liamjd.bascule.pipeline

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.liamjd.bascule.lib.model.Directories
import org.liamjd.bascule.lib.model.Project
import java.io.File
import kotlin.test.Test

class IndexPageGeneratorTest {

    private val mockFileH = mockk<org.liamjd.bascule.lib.FileHandler>()
    // This value comes from the Project constructor, and is based on System.getProperty("user.dir")
    // Which I don't like
    private var outputFile = File("C:\\Users\\liamj\\Development\\Bascule\\bascule-static\\site")
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
    val directories = Directories(
        root = File("root"),
        output = File("site"),
        assets = File("assets"),
        sources = File("sources"),
        templates = File("templates"),
        custom = null
    )


    @BeforeEach
    fun setUp() {
    }


    @AfterEach
    fun tearDown() {
    }

    @Test
    fun `build index page with no posts`() {
        // setup
        val generator = IndexPageGenerator(emptyList(), -0, 1)
        val mockRenderer = mockk<org.liamjd.bascule.lib.render.TemplatePageRenderer>()


        every { mockRenderer.render(any(), any()) } returns "<html></html>"
        every { mockFileH.writeFile(any(), any(), any()) } just Runs

        // execute
        runBlocking {
            generator.process(project, mockRenderer, mockFileH, clean = true)

            // Verify that the output file was created
            verify { mockFileH.writeFile(outputFile, "index.html", "<html></html>") }
        }
    }

}