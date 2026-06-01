package org.liamjd.bascule.flexmark.hyde

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Unit tests for [resolveInclude], the pure file-resolution step extracted from
 * [HydeTagNodeRenderer] so the found/not-found decision can be tested without an HtmlWriter.
 */
class HydeIncludeTest {

    @Test
    fun `returns Found with the file content when the file exists`(@TempDir dir: File) {
        val file = File(dir, "fragment.html")
        file.writeText("<p>included body</p>")

        val result = resolveInclude(dir.absolutePath, "fragment.html")

        assertIs<IncludeResolution.Found>(result)
        assertEquals("<p>included body</p>", result.content)
    }

    @Test
    fun `returns NotFound with the file name when the file is missing`(@TempDir dir: File) {
        val result = resolveInclude(dir.absolutePath, "missing.html")

        assertIs<IncludeResolution.NotFound>(result)
        assertEquals("missing.html", result.fileName)
    }

    @Test
    fun `resolves the file url relative to the source folder`(@TempDir dir: File) {
        val nested = File(dir, "partials").apply { mkdirs() }
        File(nested, "nav.html").writeText("NAV")

        val result = resolveInclude(dir.absolutePath, "partials/nav.html")

        assertIs<IncludeResolution.Found>(result)
        assertEquals("NAV", result.content)
    }

    @Test
    fun `a null source folder does not throw`() {
        // SOURCE_FOLDER defaults to "/"; guard against a null option without blowing up
        val result = resolveInclude(null, "definitely-not-here-12345.html")
        assertIs<IncludeResolution.NotFound>(result)
    }
}
