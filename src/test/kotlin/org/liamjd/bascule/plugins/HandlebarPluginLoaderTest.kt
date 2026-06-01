package org.liamjd.bascule.plugins

import com.vladsch.flexmark.util.misc.Extension
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [HandlebarPluginLoader]. Real classpath classes are used as plugins: [HydeExtension] is a
 * Flexmark [Extension], while [org.liamjd.bascule.model.BasculePost] is not.
 */
class HandlebarPluginLoaderTest {

    private val hydeExtension = "org.liamjd.bascule.flexmark.hyde.HydeExtension"
    private val notAnExtension = "org.liamjd.bascule.model.BasculePost"
    private val unknownClass = "com.example.DefinitelyNotARealClass"

    private fun loader(@TempDir dir: File) =
        HandlebarPluginLoader(this.javaClass.classLoader, Extension::class, dir)

    @Test
    fun `loads a valid extension class by name`(@TempDir dir: File) {
        val result = loader(dir).getExtensions(arrayListOf(hydeExtension))

        assertEquals(1, result.size)
        assertEquals(hydeExtension, result.first().qualifiedName)
    }

    @Test
    fun `ignores a class that does not implement Extension`(@TempDir dir: File) {
        val result = loader(dir).getExtensions(arrayListOf(notAnExtension))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `ignores an unknown class name`(@TempDir dir: File) {
        val result = loader(dir).getExtensions(arrayListOf(unknownClass))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `loads only the valid extensions from a mixed list`(@TempDir dir: File) {
        val result = loader(dir).getExtensions(arrayListOf(hydeExtension, notAnExtension, unknownClass))

        assertEquals(1, result.size)
        assertEquals(hydeExtension, result.first().qualifiedName)
    }
}
