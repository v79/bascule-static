package org.liamjd.bascule.assets

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.Project
import java.io.File
import kotlin.test.Test

/**
 * Tests for [AssetsProcessor]. The file handler is a mock supplied via the constructor, so the copy
 * orchestration is verified against a real fixture asset tree (src/test/resources/assets-test)
 * without writing anything to disk.
 *
 * The fixture tree is:
 * ```
 * assets-test/
 *   style.css
 *   images/
 *     logo.txt
 * ```
 */
class AssetsProcessorTest {

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
          assets: src/test/resources/assets-test
          templates: liamjd-theme/templates
          generators: [IndexPageGenerator]
    """.trimIndent()

    private val project = Project(yamlConfig = yamlConfig)
    private val mockFileHandler = mockk<BasculeFileHandler>()
    private val processor = AssetsProcessor(project, mockFileHandler)

    private fun stubFileHandler() {
        every { mockFileHandler.copyFile(any(), any()) } returns File("ignored")
        every { mockFileHandler.createDirectories(any<String>(), any<String>()) } returns File("ignored")
    }

    @Test
    fun `copies a top-level asset file into the assets output folder`() {
        stubFileHandler()

        processor.copyStatics()

        verify {
            mockFileHandler.copyFile(
                match { it.name == "style.css" },
                match { it.name == "style.css" && it.path.contains("assets") }
            )
        }
    }

    @Test
    fun `recreates nested directories and copies the files inside them`() {
        stubFileHandler()

        processor.copyStatics()

        // the "images" subfolder is recreated under the output assets folder...
        verify { mockFileHandler.createDirectories(match { it.contains("assets") }, "images") }
        // ...and its contents are copied into that nested destination
        verify {
            mockFileHandler.copyFile(
                match { it.name == "logo.txt" },
                match { it.name == "logo.txt" && it.path.contains("images") }
            )
        }
    }

    @Test
    fun `copies every file exactly once and creates each subdirectory once`() {
        stubFileHandler()

        processor.copyStatics()

        verify(exactly = 2) { mockFileHandler.copyFile(any(), any()) }
        verify(exactly = 1) { mockFileHandler.createDirectories(any<String>(), any<String>()) }
    }

    @Test
    fun `targets the output directory's assets subfolder`() {
        stubFileHandler()

        processor.copyStatics()

        // the destination root is <output>/assets, derived from project.dirs.output
        val outputName = project.dirs.output.name // "site"
        verify {
            mockFileHandler.copyFile(
                any(),
                match { it.path.contains(outputName) && it.path.contains("assets") }
            )
        }
    }
}
