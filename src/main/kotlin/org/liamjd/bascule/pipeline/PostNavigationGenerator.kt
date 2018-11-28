package org.liamjd.bascule.pipeline

import org.liamjd.bascule.FileHandler
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.generator.Post
import org.liamjd.bascule.render.Renderer
import kotlin.math.ceil
import kotlin.math.roundToInt

class PostNavigationGenerator(posts: List<Post>, numPosts: Int = 1, postsPerPage: Int) : GeneratorPipeline, AbstractPostListGenerator(posts, numPosts, postsPerPage) {

	override val TEMPLATE: String = "list"
	val FOLDER_NAME: String = "posts"

	override fun process(project: ProjectStructure, renderer: Renderer, fileHandler: FileHandler) {
		val totalPages = ceil(numPosts.toDouble() / postsPerPage).roundToInt()
		val listPages = posts.withIndex()
				.groupBy { it.index / postsPerPage }
				.map { it.value.map { it.value } }

		val postsFolder = fileHandler.createDirectory(project.outputDir.absolutePath, FOLDER_NAME)

		listPages.forEachIndexed { pageIndex, paginatedPosts ->
			val currentPage = pageIndex + 1 // to save on mangling zero-index stuff
			val model = buildPaginationModel(project.model, currentPage, totalPages, paginatedPosts, posts.size)

			val renderedContent = renderer.render(model, TEMPLATE)

			fileHandler.writeFile(postsFolder, "$TEMPLATE$currentPage.html", renderedContent)
		}
	}


}
