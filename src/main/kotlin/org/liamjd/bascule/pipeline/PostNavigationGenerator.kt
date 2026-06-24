package org.liamjd.bascule.pipeline

import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.generators.AbstractPostListGenerator
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer

/**
 * Generates paginated post navigation pages for all layouts defined in project.postLayouts, or just "post"s
 *
 * @param posts The list of posts to paginate.
 * @param numPosts The total number of posts to consider (default is 1).
 * @param postsPerPage The number of posts to display per page.
 */
class PostNavigationGenerator(posts: List<Post>, numPosts: Int = 1, postsPerPage: Int) : GeneratorPipeline,
    AbstractPostListGenerator(posts, numPosts, postsPerPage) {

    override val TEMPLATE: String = "list"

    // TODO: extend this interface to take an optional filter predicate
    // TODO: extend this interface to specify the sorting order
    override suspend fun process(
        project: Project,
        renderer: TemplatePageRenderer,
        fileHandler: FileHandler,
        clean: Boolean
    ) {
        // Filter to blog posts *before* paginating; sorting by date, then chunking guarantees full,
        // correctly ordered pages. (Indexing before filtering used to scatter posts across extra pages.)

        // Loop through all the layouts marked listed in postLayouts and run the generator for each
        // If no postLayout is specified, default to "post" layout

        if (project.postLayouts.isEmpty()) {
            project.postLayouts = setOf("post")
        }

        project.postLayouts.forEach { layout ->
            val sortedPosts = posts.filter { it.layout == layout }.sortedByDescending { it.date }
            val listPages = sortedPosts.chunked(postsPerPage)
            val totalPages = listPages.size

            if(listPages.isEmpty()) return@forEach
            val postsFolder = fileHandler.createDirectory(project.dirs.output.absolutePath, "${layout}s")

            listPages.forEachIndexed { pageIndex, paginatedPosts ->
                val currentPage = pageIndex + 1 // to save on mangling zero-index stuff
                val model = buildPaginationModel(
                    projectModel = project.model,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    posts = paginatedPosts,
                    totalPosts = sortedPosts.size,
                    layout = layout
                )
                val renderedContent = renderer.render(model, TEMPLATE)
                fileHandler.writeFile(postsFolder, "$TEMPLATE$currentPage.html", renderedContent)
            }
        }

    }


}
