package org.liamjd.bascule.scanner

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.liamjd.bascule.cache.BasculeCache
import org.liamjd.bascule.cache.CacheAndPost
import org.liamjd.bascule.cache.MDCacheItem
import org.liamjd.bascule.lib.model.PostLink
import org.liamjd.bascule.lib.model.Project
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [MarkdownScanner.calculateRenderSet]. Its collaborators (the cache and the change-set
 * calculator) are mocked, so the test exercises only the scanner's own orchestration: load -> calculate
 * -> sort/link -> write cache -> return.
 */
class MarkdownScannerTest {

	private val yamlConfig = """
        siteName: Test Site
        dateFormat: "dd/MM/yyyy"
        dateTimeFormat: HH:mm:ss dd/MM/yyyy
        author: Tester
        theme: liamjd-theme
        postsPerPage: 10
        directories:
          source: sources
          output: site
          assets: assets
          templates: liamjd-theme/templates
          generators: [IndexPageGenerator, PostNavigationGenerator, TaxonomyNavigationGenerator]
    """.trimIndent()

	private val project = Project(yamlConfig = yamlConfig).apply { clean = false }

	private val cache = mockk<BasculeCache>()
	private val changeSetCalculator = mockk<ChangeSetCalculator>()

	private val scanner = MarkdownScanner(project, cache, changeSetCalculator)

	private fun item(slug: String, date: LocalDate, layout: String = "post"): CacheAndPost {
		val md = MDCacheItem(10L, "/sources/$slug.md", LocalDateTime.of(2026, 1, 1, 0, 0))
		md.layout = layout
		md.link = PostLink(slug, "$slug.html", date)
		return CacheAndPost(md, null)
	}

	@Test
	fun `calculateRenderSet returns the uncached set produced by the change calculator`() {
		val uncached = setOf(
			item("a", LocalDate.of(2026, 1, 1)),
			item("b", LocalDate.of(2026, 2, 1))
		)
		every { cache.loadCacheFile() } returns emptySet()
		every { changeSetCalculator.calculateUncachedSet(any(), any()) } returns uncached
		every { cache.writeCacheFile(any()) } just Runs

		val result = scanner.calculateRenderSet(useCache = true)

		assertEquals(uncached, result)
	}

	@Test
	fun `the merged set of cached and new items is written back to the cache`() {
		val uncached = setOf(
			item("a", LocalDate.of(2026, 1, 1)),
			item("b", LocalDate.of(2026, 2, 1))
		)
		every { cache.loadCacheFile() } returns emptySet()
		every { changeSetCalculator.calculateUncachedSet(any(), any()) } returns uncached
		every { cache.writeCacheFile(any()) } just Runs

		scanner.calculateRenderSet(useCache = true)

		// both new items should be persisted to the cache
		verify { cache.writeCacheFile(match { items -> items.size == 2 }) }
	}

	@Test
	fun `when caching is disabled the cache file is not loaded and an empty cached set is used`() {
		every { changeSetCalculator.calculateUncachedSet(any(), any()) } returns emptySet()
		every { cache.writeCacheFile(any()) } just Runs

		scanner.calculateRenderSet(useCache = false)

		verify(exactly = 0) { cache.loadCacheFile() }
		verify { changeSetCalculator.calculateUncachedSet(emptySet(), emptySet()) }
	}
}
