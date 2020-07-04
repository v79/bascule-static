package org.liamjd.bascule.pipeline

import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.generators.AbstractPostListGenerator
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import org.liamjd.bascule.lib.generators.SortAndFilter
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import kotlin.math.ceil
import kotlin.math.roundToInt

class PostNavigationGenerator(posts: List<Post>, numPosts: Int = 1, postsPerPage: Int, private val sortAndFilter: SortAndFilter) : GeneratorPipeline, AbstractPostListGenerator(posts, numPosts, postsPerPage) {

	override val TEMPLATE: String = "list"
	private val FOLDER_NAME: String = "posts"

	override suspend fun process(project: Project, renderer: TemplatePageRenderer, fileHandler: FileHandler, clean: Boolean) {
		val totalPages = ceil(numPosts.toDouble() / postsPerPage).roundToInt()
		val listPages = sortAndFilter.sortAndFilter(project,posts)

		val postsFolder = fileHandler.createDirectory(project.dirs.output.absolutePath, FOLDER_NAME)

		listPages.forEachIndexed { pageIndex, paginatedPosts ->
			val currentPage = pageIndex + 1 // to save on mangling zero-index stuff
			val model = buildPaginationModel(project.model, currentPage, totalPages, paginatedPosts, posts.size)

			val renderedContent = renderer.render(model, TEMPLATE)

			fileHandler.writeFile(postsFolder, "$TEMPLATE$currentPage.html", renderedContent)
		}
	}


}
