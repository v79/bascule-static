package org.liamjd.bascule.pipeline

import io.mockk.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.Directories
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.model.Tag
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import org.liamjd.bascule.model.BasculePost
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

internal class MultiTaxonomyNavigationGeneratorTest : Spek({

	val mProject = mockk<Project>(relaxed = true)
	val mRenderer = mockk<TemplatePageRenderer>()
	val mFileHandler = mockk<BasculeFileHandler>()
	val mDirectories = mockk<Directories>()
	val oneTagSet = arrayListOf("genres")
	val twoTagSet = arrayListOf("genres","composers")
	val mOutputDir = mockk<File>()
	val mDirectory = mockk<File>()
	val mGenreFolder = mockk<File>(relaxed = true)
	val mComposerFolder = mockk<File>(relaxed = true)
	val mClassicalFolder = mockk<File>()
	val mClassicalFile1 = mockk<File>()

	// set up the posts.. quite a lot of data needed.
	val classicalTag = Tag(category = "genres", label = "classical", url = "classical", postCount = 1, hasPosts = false)
	val jazzTag = Tag("genres","jazz","jazz",1,false)
	val baroqueTag = Tag("genres","baroque","baroque",1,false)
	val mahlerTag = Tag("composers","mahler","mahler",1,false)
	val bachTag = Tag("composers","bach","bach",1,false)

	val postA = mockk<BasculePost>(relaxed = true)
	val postAGenres = mutableSetOf<Tag>()
	val postAClassicalTag = classicalTag.copy()
	val postAJazzTag = jazzTag.copy()
	postAGenres.addAll(setOf(postAClassicalTag,postAJazzTag))
	val postAComposers = mutableSetOf<Tag>()
	val postATags: MutableSet<Tag> = mutableSetOf()

	val postB = mockk<BasculePost>(relaxed = true)
	val postBGenres = mutableSetOf<Tag>()
	val postBClassicalTag = classicalTag.copy()
	val postBaroqueTag = baroqueTag.copy()
	postBGenres.addAll(setOf(postBClassicalTag,postBaroqueTag) )
	val postBTags: MutableSet<Tag> = mutableSetOf<Tag>()
	val postBComposers = mutableSetOf<Tag>()
	postBComposers.add(mahlerTag.copy())
	postBComposers.add(bachTag.copy())

	every { postA.layout} returns "post"
	every { postA.tags} returns postAGenres
	every { postA.title} returns "postA-classical-jazz"
	every { postB.layout} returns "post"
	every { postB.tags} returns (postBGenres + postBComposers) as MutableSet<Tag>
	every { postB.title } returns "postB-classical-baroque/mahler"
	val posts = listOf<BasculePost>(postA,postB)

	every { mProject.dirs} returns mDirectories
	every { mDirectories.output} returns mOutputDir
	every { mOutputDir.absolutePath} returns "outputPath"

	every { mRenderer.render(any(),any())} returns "fakeOutput"

	afterEachTest {
		clearMocks(mFileHandler)
	}

	describe("Can build a map with just a single default tagging") {

		it("builds a simple map") {
			every { mFileHandler.createDirectory(any(),any()) } returns mDirectory
			every { mProject.tagging} returns oneTagSet.toSet()
			every { mFileHandler.writeFile(any(), any(),any())} just Runs

			val generator = MultiTaxonomyNavigationGenerator(listOf(postA), 1, 1)

			val execute = runBlocking {
				launch {
					generator.process(project = mProject, renderer = mRenderer, fileHandler = mFileHandler, clean = true)

				}
			}
			verify(exactly = 1) { mFileHandler.createDirectory(any(), "genres") }
			verify(exactly = 0) { mFileHandler.createDirectory(any(), "composers") }
		}

	}

	describe("A project can have multiple tagging sets defined") {
		it("builds a map containing two tagging sets") {

			every { mFileHandler.createDirectory(any(),any()) } returns mDirectory
			every { mProject.tagging} returns twoTagSet.toSet()
			every { mFileHandler.createDirectory(mProject.dirs.output.absolutePath,"genres") } returns mGenreFolder
			every { mGenreFolder.absolutePath} returns "genres"
			every { mFileHandler.createDirectory(mProject.dirs.output.absolutePath,"composers") } returns mComposerFolder
			every { mComposerFolder.absolutePath} returns "composers"
			every { mFileHandler.createDirectory(mGenreFolder.absolutePath,"classical")} returns mClassicalFolder
			every { mFileHandler.writeFile(any(), any(),any())} just Runs

			val generator = MultiTaxonomyNavigationGenerator(posts.toList(),1,1)

			val execute = runBlocking {
				launch {
					generator.process(project = mProject, renderer = mRenderer, fileHandler = mFileHandler, clean = true)

				}
			}
			verify(exactly = 1) { mFileHandler.createDirectory(any(),"genres") }
			verify(exactly = 1) { mFileHandler.createDirectory(any(),"composers") }
			verify { mFileHandler.createDirectory(any(),"classical") }
			verify(exactly = 0) { mFileHandler.createDirectory(any(),"jazz") }
			verify { mFileHandler.writeFile(any(),"classical1.html", any()) }
		}
	}
})

/**
		Given:
		PostA { title, tags { genres { "jazz", "classical"}, composers { "mahler", "fitzgerald" } } }
 		PostB { title, tags { genres { "jazz", "baroque" }, composers { "bach", "fitzgerald" } } }
		PostC { title, tags { genres { "pop" }, composers {} } }
 		I want:
 		{ genres { "jazz:2", "classical:1", "baroque:1", "pop:1" }, composers { "mahler:1", "fitzgerald:2", "bach:1" } }
*/
