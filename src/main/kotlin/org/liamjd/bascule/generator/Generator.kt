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
import org.liamjd.bascule.slug
import picocli.CommandLine
import println.info
import java.io.File
import java.io.FileInputStream
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
	private val yamlConfig: String
	private val parentFolder: File
	private val configStream: FileInputStream
	private val project: ProjectStructure

	private val OUTPUT_SUFFIX = ".html"

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

		// TODO: be less agressive with this, use some sort of caching :)
		fileHandler.emptyFolder(project.outputDir, OUTPUT_SUFFIX)
		fileHandler.emptyFolder(File(project.outputDir, "tags"))
		val walker = FolderWalker(project)

		val postList = walker.generate()
		val sortedPosts = postList.sortedByDescending { it.date }
		val numPosts = sortedPosts.size

		buildIndexPage(sortedPosts, numPosts)
		buildPostNavigation(sortedPosts, numPosts)
		buildTaxonomyNavigation(sortedPosts, numPosts)
	}

	private fun buildIndexPage(posts: List<Post>, numPosts: Int = 0) {
		info("Building index file")
		val model = mutableMapOf<String, Any>()
		val postsPerPage = project.postsPerPage
		model.putAll(project.model)
		model.put("title", "Index")
		model.put("posts", posts.sortedByDescending { it.date }.take(postsPerPage))
		model.put("postCount", numPosts)
		val renderedContent = renderer.render(model, "index")

		File(project.outputDir.absolutePath, "index.html").bufferedWriter().use { out ->
			out.write(renderedContent)
		}
	}

	private fun buildTaxonomyNavigation(posts: List<Post>, numPosts: Int = 0) {
		val taxonomyMap = mutableMapOf<String, MutableList<Post>>()
		posts.forEach { post ->
			if (post.tags.isNotEmpty()) {
				for (tag in post.tags) {
					val slugTag = tag.slug()
					if (taxonomyMap[slugTag] == null) {
						taxonomyMap.put(slugTag, mutableListOf())
						taxonomyMap.get(slugTag)?.add(post)
					} else {
						taxonomyMap[slugTag]?.add(post)
					}
				}
			}
		}

		taxonomyMap.forEach { tag ->
			val postsPerPage = project.postsPerPage
			val numPosts = tag.value.size
			val totalPages = ceil(numPosts.toDouble() / postsPerPage).roundToInt()

			// only create tagged index pages if there's more than one page with the tag
			if (numPosts > 1) {
				println("Building $totalPages pages for tag ${tag.key}")
				val listPages = tag.value.withIndex()
						.groupBy { it.index / postsPerPage }
						.map { it.value.map { it.value } }

				val tagsFolder = fileHandler.createDirectory(project.outputDir.absolutePath, "tags")
				val thisTagFolder = fileHandler.createDirectory(tagsFolder.absolutePath, tag.key)

				listPages.forEachIndexed { pageIndex, taggedPosts ->

					val currentPage = pageIndex + 1
					val model = buildPaginationModel(currentPage, totalPages, taggedPosts, numPosts, tag.key.slug())
					val renderedContent = renderer.render(model, "tag")

					File(thisTagFolder, "${tag.key}$currentPage.html").bufferedWriter().use { out ->
						out.write(renderedContent)
					}
				}
			}
		}
	}

	private fun buildPostNavigation(posts: List<Post>, numPosts: Int = 0) {
		info("Building navigation lists")
		val postsPerPage = project.postsPerPage

		val totalPages = ceil(numPosts.toDouble() / postsPerPage).roundToInt()
		println("\nThere are $numPosts posts, and $postsPerPage per page. Which means $totalPages pages")

		val listPages = posts.withIndex()
				.groupBy { it.index / postsPerPage }
				.map { it.value.map { it.value } }

		val postsFolder = fileHandler.createDirectory(project.outputDir.absolutePath, "posts")
		listPages.forEachIndexed { pageIndex, paginatedPosts ->
			val currentPage = pageIndex + 1 // to save on mangling zero-index stuff
			println("Listing page $currentPage")

			val model = buildPaginationModel(currentPage, totalPages, paginatedPosts, posts.size)
			val renderedContent = renderer.render(model, "list")

			File(postsFolder, "list$currentPage.html").bufferedWriter().use { out ->
				out.write(renderedContent)
			}
		}
	}

	/**
	 * Construct pagination model for the current page in a list of posts
	 */
	private fun buildPaginationModel(currentPage: Int, totalPages: Int, posts: List<Post>, totalPosts: Int, tagLabel: String? = null): Map<String, Any> {
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
		model.put("totalPosts", totalPosts)
		model.put("posts", posts)
		model.put("pagination", buildPaginationList(currentPage, totalPages))
		if (tagLabel != null) {
			model.put("tag", tagLabel)
			model.put("title","Posts tagged '$tagLabel'")
		} else {
			model.put("title","All posts")
		}

		return model
	}

	/**
	 * This is an awful hard-coded algorithm to produce a pagination list.
	 * @param[currentPage] The current page in the list of pages
	 * @param[totalPages] Total number of pages in the list
	 * @return A list of strings representing the pagination buttons, with a specific format:
	 * _an integer_ represents a page number, e.g. 1, 2, 12...
	 * _. a period_ represents an elipsis, i.e. numbers which have been skipped, e.g 1, 2, ..., 5.
	 * _* an asterisk_, representing the current page.
	 */
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
