package org.liamjd.bascule.scanner

import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import io.mockk.every
import io.mockk.mockk
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.FakeFileScanner
import org.liamjd.bascule.cache.HandlebarsTemplateCacheItem
import org.liamjd.bascule.cache.MDCacheItem
import org.liamjd.bascule.cache.DateConversions
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostGenError
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [ChangeSetCalculator.calculateUncachedSet], driven entirely through fakes/mocks:
 * a [FakeFileScanner] supplies the (in-memory) directory tree and file metadata, and [PostBuilder] is
 * mocked so no markdown parsing / Koin YAML visitor is required.
 */
class ChangeSetCalculatorTest {

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

	private val project = Project(yamlConfig = yamlConfig)
	private val sourcesDir: File = project.dirs.sources

	private val emptyDocument = Parser.builder(MutableDataSet()).build().parse("")

	private val fileHandler = mockk<BasculeFileHandler>()
	private val postBuilder = mockk<PostBuilder>()
	private val fileScanner = FakeFileScanner()

	private val calculator = ChangeSetCalculator(project, fileHandler, postBuilder, fileScanner)

	private fun post(layout: String = "post", slug: String, title: String = slug, date: LocalDate = LocalDate.of(2026, 1, 1)): BasculePost {
		val p = BasculePost(emptyDocument)
		p.layout = layout
		p.slug = slug
		p.title = title
		p.date = date
		return p
	}

	private fun stubFileHandlerForRenderPath() {
		// getFile mirrors the real File(folder, name); readFileAsString returns canned content
		every { fileHandler.getFile(any(), any()) } answers { File(firstArg<File>(), secondArg<String>()) }
		every { fileHandler.readFileAsString(any<File>(), any<String>()) } returns "raw markdown"
	}

	@Test
	fun `new markdown files in a clean build are all flagged for rerender`() {
		project.clean = true
		stubFileHandlerForRenderPath()

		val fileA = File(sourcesDir, "a.md")
		val fileB = File(sourcesDir, "b.md")
		fileScanner
			.addDirectory(sourcesDir, listOf(fileA, fileB))
			.addFile(fileA, length = 10L, lastModified = 1_000L)
			.addFile(fileB, length = 20L, lastModified = 2_000L)

		every { postBuilder.buildPost(fileA) } returns post(slug = "a")
		every { postBuilder.buildPost(fileB) } returns post(slug = "b")

		val result = calculator.calculateUncachedSet(emptySet(), emptySet())

		assertEquals(2, result.size)
		assertTrue(result.all { it.mdCacheItem.rerender })
		// url is derived from the slug for top-level posts
		assertEquals(setOf("a.html", "b.html"), result.map { it.mdCacheItem.link.url }.toSet())
	}

	@Test
	fun `draft files, draft folders and non-markdown files are skipped`() {
		project.clean = true
		stubFileHandlerForRenderPath()

		val published = File(sourcesDir, "published.md")
		val draftFile = File(sourcesDir, ".secret.md")
		val underscoreDraft = File(sourcesDir, "__wip.md")
		val notMarkdown = File(sourcesDir, "styles.css")
		fileScanner
			.addDirectory(sourcesDir, listOf(published, draftFile, underscoreDraft, notMarkdown))
			.addFile(published, length = 10L, lastModified = 1_000L)
			.addFile(draftFile, length = 10L, lastModified = 1_000L)
			.addFile(underscoreDraft, length = 10L, lastModified = 1_000L)
			.addFile(notMarkdown, length = 10L, lastModified = 1_000L)

		every { postBuilder.buildPost(published) } returns post(slug = "published")

		val result = calculator.calculateUncachedSet(emptySet(), emptySet())

		assertEquals(1, result.size)
		assertEquals("published.html", result.first().mdCacheItem.link.url)
	}

	@Test
	fun `markdown files in nested directories are discovered recursively`() {
		project.clean = true
		stubFileHandlerForRenderPath()

		val subDir = File(sourcesDir, "blog")
		val nested = File(subDir, "nested.md")
		fileScanner
			.addDirectory(sourcesDir, listOf(subDir))
			.addDirectory(subDir, listOf(nested))
			.addFile(nested, length = 30L, lastModified = 3_000L)

		every { postBuilder.buildPost(nested) } returns post(slug = "nested")

		val result = calculator.calculateUncachedSet(emptySet(), emptySet())

		assertEquals(1, result.size)
		assertEquals("nested", result.first().post?.slug)
	}

	@Test
	fun `files that fail to parse are excluded from the change set`() {
		project.clean = true
		stubFileHandlerForRenderPath()

		val good = File(sourcesDir, "good.md")
		val bad = File(sourcesDir, "bad.md")
		fileScanner
			.addDirectory(sourcesDir, listOf(good, bad))
			.addFile(good, length = 10L, lastModified = 1_000L)
			.addFile(bad, length = 10L, lastModified = 1_000L)

		every { postBuilder.buildPost(good) } returns post(slug = "good")
		every { postBuilder.buildPost(bad) } returns PostGenError("missing title", "bad.md", "title")

		val result = calculator.calculateUncachedSet(emptySet(), emptySet())

		assertEquals(1, result.size)
		assertEquals("good.html", result.first().mdCacheItem.link.url)
	}

	@Test
	fun `an unchanged file with an unchanged template is served from cache without rerender`() {
		project.clean = false
		stubFileHandlerForRenderPath()

		val fileA = File(sourcesDir, "a.md")
		val length = 10L
		val modifiedMillis = 1_700_000_000_000L
		fileScanner
			.addDirectory(sourcesDir, listOf(fileA))
			.addFile(fileA, length = length, lastModified = modifiedMillis)

		every { postBuilder.buildPost(fileA) } returns post(slug = "a")

		// the cached item must match on path + size + modification date exactly (same derivation as walkFolder)
		val modDateTime = LocalDateTime.ofInstant(
			Instant.ofEpochMilli(modifiedMillis), TimeZone.getDefault().toZoneId()
		)
		val cachedItem = MDCacheItem(length, fileA.absolutePath, modDateTime)

		// the template is also unchanged: its cached mod-date must equal the file's lastModified (in seconds)
		val templateFile = File("templates", "post.hbs")
		every { fileHandler.getFile(project.dirs.templates, "post.hbs") } returns templateFile
		val templateLdt = LocalDateTime.of(2026, 1, 1, 12, 0, 0)
		val templateEpochSeconds = DateConversions.localDateTimeToEpochSeconds(templateLdt)
		fileScanner.addFile(templateFile, lastModified = templateEpochSeconds * 1000)
		val layoutSet = setOf(HandlebarsTemplateCacheItem("post", templateFile.path, 0L, templateLdt))

		val result = calculator.calculateUncachedSet(setOf(cachedItem), layoutSet)

		assertEquals(1, result.size)
		assertFalse(result.first().mdCacheItem.rerender, "an unchanged file+template should not be re-rendered")
	}
}
