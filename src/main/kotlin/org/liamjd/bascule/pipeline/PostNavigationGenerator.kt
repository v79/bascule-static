package org.liamjd.bascule.pipeline

import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.generators.AbstractPostListGenerator
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import kotlin.math.ceil
import kotlin.math.roundToInt

class PostNavigationGenerator(posts: List<Post>, numPosts: Int = 1, postsPerPage: Int) : GeneratorPipeline, AbstractPostListGenerator(posts, numPosts, postsPerPage) {

	override val TEMPLATE: String = "list"
	val FOLDER_NAME: String = "posts"		// TODO: move this to project model

	// TODO: extend this interface to take an optional filter predicate
	// TODO: extend this interface to specify the sorting order
	override suspend fun process(project: Project, renderer: TemplatePageRenderer, fileHandler: FileHandler, clean: Boolean) {
		val totalPages = ceil(numPosts.toDouble() / postsPerPage).roundToInt()
		val listPages = posts.reversed().withIndex()
				.filter { indexedValue: IndexedValue<Post> -> indexedValue.value.layout.equals("post") }
				.sortedByDescending { it.value.date }
				.groupBy { it.index / postsPerPage }
				.map { it.value.map { it.value } }

		val postsFolder = fileHandler.createDirectory(project.dirs.output.absolutePath, FOLDER_NAME)

		listPages.forEachIndexed { pageIndex, paginatedPosts ->
			val currentPage = pageIndex + 1 // to save on mangling zero-index stuff
			val model = buildPaginationModel(project.model, currentPage, totalPages, paginatedPosts, posts.size)

			val renderedContent = renderer.render(model, TEMPLATE)

			fileHandler.writeFile(postsFolder, "$TEMPLATE$currentPage.html", renderedContent)
		}
	}


}
