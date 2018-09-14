package org.liamjd.bascule.initializer

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.koin.dsl.module.module
import org.koin.standalone.StandAloneContext.loadKoinModules
import org.liamjd.bascule.Constants
import org.liamjd.bascule.FileHandler
import org.liamjd.bascule.assets.Theme
import java.io.File
import java.nio.file.FileSystems
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InitializerTest : Spek({

	val currentDirectory = System.getProperty("user.dir")
	val pathSeparator = FileSystems.getDefault().separator

	val name = "testSite"
	val defaultTheme: Theme = "bulma"
	val mockFileHandler = mockk<org.liamjd.bascule.FileHandler>(relaxed = true)

	val koinModule = module {
		single { mockFileHandler }
	}
	loadKoinModules(koinModule)


	describe("BasculeInitializer sets up an empty project with the given name") {

		afterEachTest {
			val testDirectory = System.getProperty("user.dir")
			val testSeparator = FileSystems.getDefault().separator
			val deleteMe = File("$testDirectory$testSeparator$name")
			println("Tests done - deleting $deleteMe")
			deleteMe.deleteRecursively()
		}
		val mRootFile = File(name)


		it("fails if it cannot create the root folder") {

			every { mockFileHandler.createDirectories(mRootFile) } returns false
			val service = BasculeInitializer(name, defaultTheme, mockFileHandler)
			service.create()

			val shouldNotExist = File("$currentDirectory$pathSeparator$name")
			assertFalse { shouldNotExist.exists() }
		}

		it("successfully creates a project called $name with theme 'unthemed'") {
			val unthemed = "unthemed"
			val spyFileHandler = spyk(FileHandler())

			val service = BasculeInitializer(name, unthemed, spyFileHandler)
			service.create()

			// verification
			val siteRoot = File("$currentDirectory$pathSeparator$name")
			assertTrue { siteRoot.exists() }
			verify(atMost = 4) { spyFileHandler.createDirectory(any(),any()) }
			verify(exactly = 1) { spyFileHandler.copyFileFromResources(fileName = "post.html", destination = any(), sourceDir = "${Constants.THEME_FOLDER}/$unthemed/templates/") }
		}

		it("successfully creates a project called $name with default 'bulma' theme when no theme is specified") {

			val mockFileHandler = spyk(FileHandler())

			val service = BasculeInitializer(name, null, mockFileHandler)
			service.create()

			// verification
			val siteRoot = File("$currentDirectory$pathSeparator$name")
			assertTrue { siteRoot.exists() }
			verify(atMost = 4) { mockFileHandler.createDirectory(any(),any()) }
			verify(exactly = 1) { mockFileHandler.copyFileFromResources(fileName = "post.html", destination = any<File>(), sourceDir = "${Constants.THEME_FOLDER}/${defaultTheme}/templates/") }
		}
	}
})