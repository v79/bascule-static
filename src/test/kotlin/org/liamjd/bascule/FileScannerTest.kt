package org.liamjd.bascule

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies the real [FileScanner] implementation on [BasculeFileHandler] against an actual temp
 * directory, and that the in-memory [FakeFileScanner] models the same contract for unit tests.
 */
class FileScannerTest {

	private val handler = BasculeFileHandler()

	// ---- real implementation, backed by a temp directory ------------------

	@Test
	fun `listFiles returns the immediate children of a directory`(@TempDir tempDir: File) {
		File(tempDir, "a.md").writeText("hello")
		File(tempDir, "b.md").writeText("world")
		File(tempDir, "sub").mkdir()

		val names = handler.listFiles(tempDir).map { it.name }.toSet()
		assertEquals(setOf("a.md", "b.md", "sub"), names)
	}

	@Test
	fun `isDirectory distinguishes directories from files`(@TempDir tempDir: File) {
		val file = File(tempDir, "post.md").apply { writeText("x") }
		val dir = File(tempDir, "folder").apply { mkdir() }

		assertTrue(handler.isDirectory(dir))
		assertFalse(handler.isDirectory(file))
	}

	@Test
	fun `length reports the file size in bytes`(@TempDir tempDir: File) {
		val file = File(tempDir, "post.md").apply { writeText("hello") } // 5 bytes
		assertEquals(5L, handler.length(file))
	}

	@Test
	fun `lastModified is populated for a real file`(@TempDir tempDir: File) {
		val file = File(tempDir, "post.md").apply { writeText("x") }
		assertTrue(handler.lastModified(file) > 0L)
	}

	@Test
	fun `listFiles returns empty for a non-directory or missing path`(@TempDir tempDir: File) {
		val file = File(tempDir, "post.md").apply { writeText("x") }
		assertTrue(handler.listFiles(file).isEmpty())
		assertTrue(handler.listFiles(File(tempDir, "does-not-exist")).isEmpty())
	}

	// ---- the fake honours the same contract -------------------------------

	@Test
	fun `FakeFileScanner models directories, children and metadata`() {
		val root = File("/sources")
		val postA = File("/sources/a.md")
		val fake = FakeFileScanner()
			.addDirectory(root, listOf(postA))
			.addFile(postA, length = 42L, lastModified = 1000L)

		assertEquals(listOf(postA), fake.listFiles(root))
		assertTrue(fake.isDirectory(root))
		assertFalse(fake.isDirectory(postA))
		assertEquals(42L, fake.length(postA))
		assertEquals(1000L, fake.lastModified(postA))
		// unknown path: empty / non-directory / zero
		assertTrue(fake.listFiles(File("/nowhere")).isEmpty())
		assertFalse(fake.isDirectory(File("/nowhere")))
		assertEquals(0L, fake.length(File("/nowhere")))
	}
}
