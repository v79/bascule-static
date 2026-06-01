package org.liamjd.bascule.plugins

import org.junit.jupiter.api.io.TempDir
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [GeneratorPluginLoader]. Real classpath classes are used as plugins: [org.liamjd.bascule.pipeline.IndexPageGenerator]
 * is a [GeneratorPipeline], while [org.liamjd.bascule.model.BasculePost] is not.
 */
class GeneratorPluginLoaderTest {

    private val indexGenerator = "org.liamjd.bascule.pipeline.IndexPageGenerator"
    private val notAGenerator = "org.liamjd.bascule.model.BasculePost"
    private val unknownClass = "com.example.DefinitelyNotARealClass"

    private fun loader(@TempDir dir: File) =
        GeneratorPluginLoader(this.javaClass.classLoader, GeneratorPipeline::class, dir)

    @Test
    fun `loads a valid generator class by name`(@TempDir dir: File) {
        val result = loader(dir).getGenerators(listOf(indexGenerator))

        assertEquals(1, result.size)
        assertEquals(indexGenerator, result.first().qualifiedName)
    }

    @Test
    fun `ignores a class that does not implement GeneratorPipeline`(@TempDir dir: File) {
        val result = loader(dir).getGenerators(listOf(notAGenerator))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `ignores an unknown class name`(@TempDir dir: File) {
        val result = loader(dir).getGenerators(listOf(unknownClass))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns an empty list for a null generator list`(@TempDir dir: File) {
        val result = loader(dir).getGenerators(null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `loads only the valid generators from a mixed list`(@TempDir dir: File) {
        val result = loader(dir).getGenerators(listOf(indexGenerator, notAGenerator, unknownClass))

        assertEquals(1, result.size)
        assertEquals(indexGenerator, result.first().qualifiedName)
    }
}
