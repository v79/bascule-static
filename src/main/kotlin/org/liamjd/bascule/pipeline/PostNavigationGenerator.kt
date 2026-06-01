package org.liamjd.bascule.pipeline

import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.generators.AbstractPostListGenerator
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer

class PostNavigationGenerator(posts: List<Post>, numPosts: Int = 1, postsPerPage: Int) : GeneratorPipeline, AbstractPostListGenerator(posts, numPosts, postsPerPage) {

	override val TEMPLATE: String = "list"
	val FOLDER_NAME: String = "posts"		// TODO: move this to project model

	// TODO: extend this interface to take an optional filter predicate
	// TODO: extend this interface to specify the sorting order
	override suspend fun process(project: Project, renderer: TemplatePageRenderer, fileHandler: FileHandler, clean: Boolean) {
		// Filter to blog posts *before* paginating; sorting by date then chunking guarantees full,
		// correctly-ordered pages. (Indexing before filtering used to scatter posts across extra pages.)
		val sortedPosts = posts.filter { it.layout.equals("post") }.sortedByDescending { it.date }
		val listPages = sortedPosts.chunked(postsPerPage)
		val totalPages = listPages.size

		val postsFolder = fileHandler.createDirectory(project.dirs.output.absolutePath, FOLDER_NAME)

		listPages.forEachIndexed { pageIndex, paginatedPosts ->
			val currentPage = pageIndex + 1 // to save on mangling zero-index stuff
			val model = buildPaginationModel(project.model, currentPage, totalPages, paginatedPosts, sortedPosts.size)

			val renderedContent = renderer.render(model, TEMPLATE)

			fileHandler.writeFile(postsFolder, "$TEMPLATE$currentPage.html", renderedContent)
		}
	}


}
