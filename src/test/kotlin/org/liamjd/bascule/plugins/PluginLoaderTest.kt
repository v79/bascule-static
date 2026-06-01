package org.liamjd.bascule.plugins

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URLClassLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for the shared behaviour on the abstract [PluginLoader] base class: JAR discovery and the
 * construction of the JAR class loader.
 */
class PluginLoaderTest {

    // PluginLoader is abstract with no abstract members, so a bodyless subclass is enough to exercise it
    private class TestPluginLoader(classLoader: ClassLoader, pluginFolder: File) :
        PluginLoader(classLoader, pluginFolder)

    private val parentClassLoader = this.javaClass.classLoader

    private fun loader(folder: File) = TestPluginLoader(parentClassLoader, folder)

    @Test
    fun `addJars collects every jar in the tree and ignores other files`(@TempDir dir: File) {
        File(dir, "alpha.jar").writeText("")
        File(dir, "notes.txt").writeText("")
        val sub = File(dir, "nested").apply { mkdirs() }
        File(sub, "beta.jar").writeText("")

        val loader = loader(dir)
        loader.addJars(dir)

        assertEquals(2, loader.jars.size)
        val names = loader.jars.map { File(it.toURI()).name }.toSet()
        assertEquals(setOf("alpha.jar", "beta.jar"), names)
    }

    @Test
    fun `addJars adds nothing for an empty folder`(@TempDir dir: File) {
        val loader = loader(dir)
        loader.addJars(dir)
        assertTrue(loader.jars.isEmpty())
    }

    @Test
    fun `addJars adds nothing for a folder that does not exist`(@TempDir dir: File) {
        val loader = loader(dir)
        loader.addJars(File(dir, "missing-folder"))
        assertTrue(loader.jars.isEmpty())
    }

    @Test
    fun `loadPlugins returns null for a null class list`(@TempDir dir: File) {
        assertNull(loader(dir).loadPlugins(null))
    }

    @Test
    fun `loadPlugins returns null for an empty class list`(@TempDir dir: File) {
        // documents the current behaviour: with no class names the loop never builds a loader
        assertNull(loader(dir).loadPlugins(emptyList()))
    }

    @Test
    fun `loadPlugins returns a URLClassLoader parented to the supplied loader for a non-empty list`(@TempDir dir: File) {
        val loader = loader(dir)

        val result = loader.loadPlugins(listOf("com.example.SomePlugin"))

        assertTrue(result is URLClassLoader)
        assertSame(parentClassLoader, result.parent)
    }
}
