package org.liamjd.bascule.scanner

import io.mockk.every
import io.mockk.mockk
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.model.Directories
import org.liamjd.bascule.lib.model.Project
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


internal class BasculeCacheImplTest : Spek({

	val mProject = mockk<Project>(relaxed = true)
	val mDirectories = mockk<Directories>()
	val mFileHandler = mockk<FileHandler>()
	val mSourceFile = mockk<File>()
	val mJsonStream = mockk<InputStream>()

	describe("Read a json string and return it as a Set of MDCacheItems") {

		every { mProject.name } returns TEST_DATA.test_project_name
		every { mDirectories.sources } returns mSourceFile

		it("creates single item for Review of Big Bang") {

			every { mFileHandler.getFileStream(any(), "${TEST_DATA.test_project_name}.cache.json") } returns mJsonStream

			every { mJsonStream.toString() } returns TEST_DATA.big_bang_json

			val cacher = BasculeCacheImpl(mProject, mFileHandler)

			val result = cacher.loadCacheFile()

			assertNotNull(result, "Cache returned non-null object")
			assertEquals(1, result.size,"Cache returned a single object")
			assertNotNull(result.first()) { item ->
				assertEquals(TEST_DATA.big_bang_title, item.link.title)
				assertEquals(TEST_DATA.big_bang_url, item.link.url)
				assertEquals(TEST_DATA.big_bang_date,item.link.date)
			}
		}
	}

})

object TEST_DATA {
	val test_project_name = "test-project"

	val big_bang_title = "Review of Big Bang by Simon Singh"
	val big_bang_url = "2005/review-of-big-bang.html"
	val big_bang_date = LocalDate.of(2005, Month.OCTOBER,8)
	val big_bang_json = """
	[
		{
			"sourceFileSize": 1061,
			"sourceFilePath": "D:\\Development\\liamjdavison\\sources\\2005\\Review of Big Bang.md",
			"sourceModificationDate": "2018-09-11T21:32:30.342",
			"link": {
				"title": "Review of Big Bang by Simon Singh",
				"url": "2005/review-of-big-bang.html",
				"date": 1128726000
			},
			"tags": [
			],
			"previous": null,
			"next": {
				"title": "Single Syllable Story",
				"url": "2005/single-syllable-story.html",
				"date": 1128726000
			},
			"layout": "post",
			"rerender": true
		}
	]
	""".trimIndent()
}
