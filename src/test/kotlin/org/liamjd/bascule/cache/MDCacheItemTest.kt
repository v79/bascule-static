package org.liamjd.bascule.cache

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * [MDCacheItem] deliberately bases equality and hashCode on the source file path alone, so that it can
 * be used in a [Set] keyed by file. These tests pin down that contract.
 */
class MDCacheItemTest {

	private val modDate = LocalDateTime.of(2026, 5, 31, 12, 0, 0)

	private fun item(path: String, size: Long = 100L) =
		MDCacheItem(sourceFileSize = size, sourceFilePath = path, sourceModificationDate = modDate)

	@Test
	fun `items with the same source path are equal regardless of size`() {
		val a = item("/sources/post.md", size = 100L)
		val b = item("/sources/post.md", size = 999L)
		assertEquals(a, b)
	}

	@Test
	fun `items with different source paths are not equal`() {
		val a = item("/sources/one.md")
		val b = item("/sources/two.md")
		assertNotEquals(a, b)
	}

	@Test
	fun `hashCode is derived solely from the source path`() {
		val a = item("/sources/post.md", size = 100L)
		val b = item("/sources/post.md", size = 42L)
		assertEquals(a.hashCode(), b.hashCode())
	}

	@Test
	fun `a Set deduplicates items that share a source path`() {
		val set = setOf(item("/sources/one.md"), item("/sources/one.md", size = 5L))
		assertEquals(1, set.size)
	}

	@Test
	fun `a Set keeps items with distinct source paths`() {
		val set = setOf(item("/sources/one.md"), item("/sources/two.md"))
		assertEquals(2, set.size)
	}

	@Test
	fun `an item is not equal to null or to other types`() {
		val a = item("/sources/one.md")
		assertNotEquals<Any?>(a, null)
		assertFalse(a.equals("/sources/one.md"))
	}
}
