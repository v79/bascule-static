package org.liamjd.bascule.scanner

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext
import org.liamjd.bascule.lib.model.Directories
import org.liamjd.bascule.lib.model.Project
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertNotNull

internal class ChangeSetCalculatorTest  : Spek({

	val mProject = mockk<Project>(relaxed = true)
	val mCache = mockk<BasculeCache>()
	val mockFileHandler = mockk<org.liamjd.bascule.BasculeFileHandler>(relaxed = true)
	val mDirectories = mockk<Directories>()
	val mSourceFile = mockk<File>()

	val koinModule = module {
		single { mockFileHandler }
		single { mCache }
	}
	StandAloneContext.loadKoinModules(koinModule)

	every { mProject.name} returns "Test project"
	every { mDirectories.sources } returns mSourceFile

	describe("Creating a cache file when no cache exists") {

		every { mCache.loadCacheFile()} returns emptySet()
		every { mCache.writeCacheFile(any())} returns Unit

		it("will create file test-project.cache.json") {

			val scanner = MarkdownScanner(mProject)

			val resultSet = scanner.calculateRenderSet()

			assertNotNull(resultSet)
			verify(exactly = 1) { mCache.writeCacheFile(any()) }
			verify() { mockFileHandler.writeFile(any(),"test-project.cache.json",any())}
		}
	}

}) {

	object TEST_DATA {
		val big_bang_title = "Review of Big Bang by Simon Singh"
		val big_bang_url = "2005/review-of-big-bang.html"
		val big_bang_date = LocalDate.of(2005, Month.OCTOBER,8)

		val bigBangItem = MDCacheItem(12345L, big_bang_url,big_bang_date.atStartOfDay())

		val mdCacheItemSet = setOf(bigBangItem)

	}
}
