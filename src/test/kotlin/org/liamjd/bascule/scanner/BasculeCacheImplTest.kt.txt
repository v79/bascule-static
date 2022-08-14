package org.liamjd.bascule.scanner

import io.mockk.every
import io.mockk.mockk
import org.liamjd.bascule.cache.BasculeCacheImpl
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.model.Directories
import org.liamjd.bascule.lib.model.Project
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class BasculeCacheImplTest : Spek({

	val mProject = mockk<Project>(relaxed = true)
	val mDirectories = mockk<Directories>()
	val mFileHandler = mockk<FileHandler>()
	val mSourceFile = mockk<File>()

	describe("Creating a cache file when no cache exists") {

		every { mFileHandler.readFileAsString(any(), "${TEST_DATA.test_project_name}.cache.json")} throws (FileNotFoundException())

		it("will return an empty set when no cache file exists") {

			val cacher = BasculeCacheImpl(mProject, mFileHandler)

			val result = cacher.loadCacheFile()

			assertNotNull(result) {
				assertEquals(0,it.size)
			}
//			verify { mFileHandler.writeFile(any(),"test-data.cache.json",any())}
		}

	}

	describe("Read a json string and return it as a Set of MDCacheItems") {

		every { mProject.name } returns TEST_DATA.test_project_name
		every { mDirectories.sources } returns mSourceFile

		it("creates single item for Review of Big Bang") {

			every { mFileHandler.readFileAsString(any(), "${TEST_DATA.test_project_name}.cache.json") } returns TEST_DATA.big_bang_json

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

