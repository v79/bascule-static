package org.liamjd.bascule.scanner

import org.liamjd.bascule.cache.CacheAndPost
import org.liamjd.bascule.cache.MDCacheItem
import org.liamjd.bascule.lib.model.PostLink
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the pure helpers extracted from the scanner. No filesystem or Koin required.
 */
class ScannerFunctionsTest {

	// ---- isDraftName ------------------------------------------------------

	@Test
	fun `names starting with a dot are drafts`() = assertTrue(isDraftName(".draft.md"))

	@Test
	fun `names starting with double underscore are drafts`() = assertTrue(isDraftName("__wip"))

	@Test
	fun `ordinary names are not drafts`() = assertFalse(isDraftName("published.md"))

	@Test
	fun `a single underscore is not a draft`() = assertFalse(isDraftName("_partial.md"))

	// ---- isMarkdownFile ---------------------------------------------------

	@Test
	fun `lowercase md extension is markdown`() = assertTrue(isMarkdownFile(File("post.md")))

	@Test
	fun `uppercase MD extension is markdown`() = assertTrue(isMarkdownFile(File("POST.MD")))

	@Test
	fun `non-md extensions are not markdown`() {
		assertFalse(isMarkdownFile(File("style.css")))
		assertFalse(isMarkdownFile(File("noextension")))
	}

	// ---- calculateUrl -----------------------------------------------------

	@Test
	fun `url for an empty source path is just the slug`() {
		assertEquals("my-post.html", calculateUrl("my-post", ""))
	}

	@Test
	fun `url for a nested source path is normalised to forward slashes`() {
		assertEquals("blog/2026/my-post.html", calculateUrl("my-post", "\\blog\\2026"))
	}

	@Test
	fun `url strips a single leading backslash from the source path`() {
		assertEquals("blog/my-post.html", calculateUrl("my-post", "\\blog"))
	}

	// ---- cacheContainsItem ------------------------------------------------

	private val modDate = LocalDateTime.of(2026, 5, 31, 12, 0, 0)
	private fun item(path: String, size: Long = 100L, date: LocalDateTime = modDate) =
		MDCacheItem(sourceFileSize = size, sourceFilePath = path, sourceModificationDate = date)

	@Test
	fun `cache hit when path, size and date all match`() {
		val cached = setOf(item("/sources/a.md"))
		assertTrue(cacheContainsItem(item("/sources/a.md"), cached))
	}

	@Test
	fun `cache miss when the size differs`() {
		val cached = setOf(item("/sources/a.md", size = 100L))
		assertFalse(cacheContainsItem(item("/sources/a.md", size = 200L), cached))
	}

	@Test
	fun `cache miss when the modification date differs`() {
		val cached = setOf(item("/sources/a.md", date = modDate))
		assertFalse(cacheContainsItem(item("/sources/a.md", date = modDate.plusSeconds(1)), cached))
	}

	@Test
	fun `cache miss when the path is unknown`() {
		val cached = setOf(item("/sources/a.md"))
		assertFalse(cacheContainsItem(item("/sources/b.md"), cached))
	}

	// ---- sortAndLinkPosts -------------------------------------------------

	private fun post(path: String, date: LocalDate, layout: String = "post"): CacheAndPost {
		val mdItem = item(path)
		mdItem.layout = layout
		mdItem.link = PostLink(path, "$path.html", date)
		return CacheAndPost(mdItem, post = null)
	}

	@Test
	fun `posts are sorted ascending by date`() {
		val unsorted = setOf(
			post("c", LocalDate.of(2026, 3, 1)),
			post("a", LocalDate.of(2026, 1, 1)),
			post("b", LocalDate.of(2026, 2, 1))
		)
		val ordered = sortAndLinkPosts(unsorted).map { it.mdCacheItem.link.title }
		assertEquals(listOf("a", "b", "c"), ordered)
	}

	@Test
	fun `previous and next links are wired across the ordered posts`() {
		val a = post("a", LocalDate.of(2026, 1, 1))
		val b = post("b", LocalDate.of(2026, 2, 1))
		val c = post("c", LocalDate.of(2026, 3, 1))

		sortAndLinkPosts(setOf(c, a, b))

		// middle post points both ways
		assertEquals("a.html", b.mdCacheItem.previous?.url)
		assertEquals("c.html", b.mdCacheItem.next?.url)
		// ends are open
		assertNull(a.mdCacheItem.previous)
		assertNull(c.mdCacheItem.next)
		assertEquals("b.html", a.mdCacheItem.next?.url)
		assertEquals("b.html", c.mdCacheItem.previous?.url)
	}

	@Test
	fun `non-post layouts are excluded from the navigation chain`() {
		val page = post("about", LocalDate.of(2026, 1, 15), layout = "page")
		val p1 = post("first", LocalDate.of(2026, 1, 1))
		val p2 = post("second", LocalDate.of(2026, 2, 1))

		val result = sortAndLinkPosts(setOf(page, p1, p2))

		// the page is still in the sorted set ...
		assertTrue(result.any { it.mdCacheItem.link.title == "about" })
		// ... but is not linked, and the two posts link directly to each other
		assertNull(page.mdCacheItem.previous)
		assertNull(page.mdCacheItem.next)
		assertEquals("second.html", p1.mdCacheItem.next?.url)
		assertEquals("first.html", p2.mdCacheItem.previous?.url)
	}
}
