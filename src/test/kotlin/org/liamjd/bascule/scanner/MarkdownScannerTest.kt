package org.liamjd.bascule.scanner

import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.UnstableDefault
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.loadKoinModules
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.Directories
import org.liamjd.bascule.lib.model.Project
import java.io.File

private const val PROJECT_NAME = "testProject"


@UseExperimental(UnstableDefault::class)
class MarkdownScannerTest : Spek( {

	val mockFileHandler = mockk<BasculeFileHandler>(relaxed = true)

	val mockProject = mockk<Project>()
	val mDirectories = mockk<Directories>()
	val mSourceFile = mockk<File>()

	val koinModule = module {
		single { mockFileHandler }
	}
	loadKoinModules(koinModule)

	describe("Markdown scanner iterates through the source file and works out what needs to be rendered") {

		beforeEachTest {
			every { mockProject.name} returns PROJECT_NAME
			every { mockProject.dirs } returns mDirectories
			every { mDirectories.sources} returns mSourceFile
		}

		it("loads a cache file from json") {

			val scanner = MarkdownScanner(mockProject)
			val cacheAndPostSet = scanner.calculateRenderSet()

		}

	}
})
