package org.liamjd.bascule.scanner

import io.mockk.every
import io.mockk.mockk
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext
import org.liamjd.bascule.cache.MDCacheItem
import org.liamjd.bascule.lib.model.Directories
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.model.BasculePost
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class ChangeSetCalculatorTest : Spek({

	val mProject = mockk<Project>()
	val mDirectories = mockk<Directories>()
	val mSourceDirectory = mockk<File>(name = "mSourceDirectory", relaxed = true)
	val mOutputDirectory = mockk<File>(name = "mOutputDirectory")
	val mTemplatesDirectory = mockk<File>(name = "mTemplateDirectory")
	val mockFileHandler = mockk<org.liamjd.bascule.BasculeFileHandler>(relaxed = true)
	val mPostBuilder = mockk<PostBuilder>()
	val mParent = mockk<File>(relaxed = true)
	val mHBTemplateFile = mockk<File>(relaxed = true)

	val koinModule = module {
		single(override = true) { mockFileHandler }
		single { mPostBuilder }
	}
	StandAloneContext.loadKoinModules(koinModule)

	every { mProject.name } returns TEST_DATA.test_project_name
	every { mProject.dirs } returns mDirectories
	every { mProject.clean } returns false
	every { mDirectories.sources } returns mSourceDirectory
	every { mDirectories.output } returns mOutputDirectory
	every { mDirectories.templates} returns mTemplatesDirectory

	describe("Given no cache file and no source files") {
		beforeEachTest {
			every { mSourceDirectory.absolutePath } returns "scannertests/empty"
			every { mSourceDirectory.isDirectory } returns true
			every { mSourceDirectory.parentFile } returns mParent
			every { mParent.name } returns "scannertests"
		}
		it("should return an empty change set") {
			val calculator = ChangeSetCalculator(mProject)
			val result = calculator.calculateUncachedSet(TEST_DATA.mdCacheItemSet, TEST_DATA.hbTemplateCacheEmptySet)

			assertNotNull(result) {
				assertEquals(0, it.size)
			}
		}
	}

	describe("Given no existing cache, and a single source file") {

		val reviewOfBigBangContent = File(this::class.java.classLoader.getResource("scannertests/bigBang/Review of Big Bang.md").toURI())
		val fileList = mutableListOf<File>()

		fileList.add(reviewOfBigBangContent)

		val mPost = mockk<BasculePost>(relaxed = true)

		beforeEachTest {
			every { mockFileHandler.getFile(any(), any()) } returns mOutputDirectory
			every { mockFileHandler.readFileAsString(reviewOfBigBangContent.parentFile, reviewOfBigBangContent.name) } returns reviewOfBigBangContent.readText()
			every { mPostBuilder.buildPost(reviewOfBigBangContent) } returns mPost
			every { mSourceDirectory.listFiles() } returns fileList.toTypedArray()
			every { mSourceDirectory.parentFile } returns mParent
			every { mParent.path } returns "scannertests"
			every { mOutputDirectory.parent } returns "scannertests"
			every { mOutputDirectory.absolutePath } returns "scannertests/bigBang/"
		}

		it("should return a change set with a single file; rerender is true") {

			val calculator = ChangeSetCalculator(mProject)
			val result = calculator.calculateUncachedSet(TEST_DATA.mdCacheItemEmptySet, setOf(TEST_DATA.hbTemplatePostItem))

			assertNotNull(result) {
				assertEquals(1, it.size)
				assertNotNull(it.first()) {
					assertTrue(it.mdCacheItem.rerender)
				}
			}
		}
	}

	describe("Given a cache file, and a single source file") {

		val reviewOfBigBangContent = File(this::class.java.classLoader.getResource("scannertests/bigBang/Review of Big Bang.md").toURI())
		val fileList = mutableListOf<File>()

		fileList.add(reviewOfBigBangContent)

		// setting up the cache object means recreating the mdItem exactly from the source file
		val mPost = mockk<BasculePost>(relaxed = true)
		var bigBangTestSource_lastModified = reviewOfBigBangContent.lastModified()
		var cacheItem = MDCacheItem(reviewOfBigBangContent.length(), reviewOfBigBangContent.absolutePath, LocalDateTime.ofInstant(Instant.ofEpochMilli(bigBangTestSource_lastModified), TimeZone
				.getDefault().toZoneId()))
		val cacheSet = setOf(cacheItem)

		beforeEachTest {
			every { mPost.layout } returns TEST_DATA.hbTemplatePostItem.layoutName
			every { mockFileHandler.getFile(any(), any()) } returns mOutputDirectory
			every { mockFileHandler.readFileAsString(reviewOfBigBangContent.parentFile, reviewOfBigBangContent.name) } returns reviewOfBigBangContent.readText()
			every { mockFileHandler.getFile(any(), any())} returns mHBTemplateFile
			every { mPostBuilder.buildPost(reviewOfBigBangContent) } returns mPost
			every { mSourceDirectory.listFiles() } returns fileList.toTypedArray()
			every { mSourceDirectory.parentFile } returns mParent
			every { mParent.path } returns "scannertests"
			every { mOutputDirectory.parent } returns "scannertests"
			every { mOutputDirectory.absolutePath } returns "scannertests/bigBang/"
			every { mHBTemplateFile.lastModified()} returns TEST_DATA.post_template_modification_date_MILLIS
		}

		it("will return a set of items with the rerender flag set to false when the source matches the cache") {
			val calculator = ChangeSetCalculator(mProject)
			val result = calculator.calculateUncachedSet(cacheSet,setOf(TEST_DATA.hbTemplatePostItem))

			assertNotNull(result) {
				assertEquals(1, it.size)
				assertNotNull(it.first()) {
					assertFalse(it.mdCacheItem.rerender)
				}
			}
		}
	}

})
