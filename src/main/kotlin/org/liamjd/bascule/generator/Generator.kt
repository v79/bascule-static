package org.liamjd.bascule.generator

import org.koin.core.parameter.ParameterList
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.Constants
import org.liamjd.bascule.FileHandler
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.random
import org.liamjd.bascule.render.Renderer
import org.liamjd.bascule.scanner.FolderWalker
import picocli.CommandLine
import println.info
import java.io.File
import java.io.FileInputStream
import java.nio.file.FileSystems
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Starts the post and page generation process. Must be run from inside the project folder
 */
@CommandLine.Command(name = "generate", description = ["Generate your static website"])
class Generator : Runnable, KoinComponent {

	private val fileHandler: FileHandler by inject(parameters = { ParameterList() })
	private val renderer by inject<Renderer> { parametersOf(project) }

	private val currentDirectory = System.getProperty("user.dir")!!
	private val pathSeparator = FileSystems.getDefault().separator
	private val yamlConfig: String
	private val parentFolder: File
	private val configStream: FileInputStream
	private val project: ProjectStructure

	init {
		parentFolder = File(currentDirectory)
		yamlConfig = "${parentFolder.name}.yaml"

		configStream = File(parentFolder.absolutePath, yamlConfig).inputStream()
		project = ProjectStructure.Configurator.buildProjectFromYamlConfig(configStream)
	}

	override fun run() {

		info(Constants.logos[(0 until Constants.logos.size).random()])
		info("Generating your website")
		info("Reading yaml configuration file $yamlConfig")

		val walker = FolderWalker(project)

		val postList = walker.generate()
		val numPosts = postList.size

		buildIndex(postList, numPosts)
		buildPostNavigation(postList, numPosts)
	}

	private fun buildIndex(posts: List<Post>, numPosts: Int = 0) {
		info("Building index file")
		val model = mutableMapOf<String, Any>()
		val postsPerPage = project.postsPerPage
		model.putAll(project.model)
		model.put("posts", posts.sortedByDescending { it.date }.take(postsPerPage))
		model.put("postCount", numPosts)
		val renderedContent = renderer.render(model, "index")

//		println("index -> $renderedContent")

		File(project.outputDir.absolutePath, "index.html").bufferedWriter().use { out ->
			out.write(renderedContent)
		}
	}

	private fun buildPostNavigation(posts: List<Post>, numPosts: Int = 0) {
		info("Building navigation lists")
		val model = mutableMapOf<String, Any>()
		val postsPerPage = project.postsPerPage
		model.putAll(project.model)
		val sortedPosts = posts.sortedByDescending { it.date }

		val totalPages = ceil(numPosts.toDouble() / postsPerPage).roundToInt()
		println("\nThere are $numPosts posts, and $postsPerPage per page. Which means $totalPages pages")
		val postCounter = 0

		val listPages = sortedPosts.withIndex()
				.groupBy { it.index / postsPerPage }
				.map { it.value.map { it.value } }

		val postsFolder = fileHandler.createDirectory(project.outputDir.absolutePath, "posts")
		listPages.forEachIndexed { pageIndex, posts ->
			val currentPage = pageIndex + 1 // to save on mangling zero-index stuff
			println("Listing page $currentPage")

			val model = mutableMapOf<String, Any>()
			model.putAll(project.model)
			model.put("currentPage", currentPage)
			model.put("totalPages", totalPages)
			model.put("isFirst", currentPage == 1)
			model.put("isLast", currentPage >= totalPages)
			model.put("previousPage", currentPage - 1)
			model.put("nextPage", currentPage + 1)
			model.put("nextIsLast", currentPage == totalPages)
			model.put("prevIsFirst", (currentPage - 1) == 1)


			model.put("posts", posts)

			model.put("pagination", buildPaginationList(currentPage, totalPages))
			val renderedContent = renderer.render(model, "list")

			File(postsFolder, "list${currentPage}.html").bufferedWriter().use { out ->
				out.write(renderedContent)
			}
		}
	}

	private fun buildPaginationList(currentPage: Int, totalPages: Int): List<String> {
		val paginationList = mutableListOf<String>()
		val prev = currentPage - 1
		val next = currentPage + 1
		val isFirst = (currentPage == 1)
		val isLast = (currentPage == totalPages)
		val prevIsFirst = (currentPage - 1 == 1)
		val nextIsLast = (currentPage + 1 == totalPages)

		if (totalPages == 1) {
			paginationList.add("*")
		} else if (isFirst) {
			paginationList.add("*")
			if (nextIsLast) {
				paginationList.add("$next")
			} else {
				paginationList.add("$next")
				paginationList.add(".")
				paginationList.add("$totalPages")
			}
		} else if (prevIsFirst) {
			paginationList.add("1")
			paginationList.add("*")
			if (!isLast) {
				paginationList.add(".")
				paginationList.add("$totalPages")
			}
		} else if (!isLast) {
			paginationList.add("1")
			paginationList.add(".")
			paginationList.add("$prev")
			paginationList.add("*")
			if (!nextIsLast) {
				paginationList.add("$next")
				paginationList.add(".")
				paginationList.add("$totalPages")
			} else {
				paginationList.add("$totalPages")
			}
		} else if (isLast) {
			paginationList.add("1")
			paginationList.add(".")
			paginationList.add("$prev")
			paginationList.add("*")
		}
		return paginationList
	}

}
