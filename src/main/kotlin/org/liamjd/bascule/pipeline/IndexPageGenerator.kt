package org.liamjd.bascule.pipeline

import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.generators.AbstractPostListGenerator
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.Renderer
import println.info

/**
 * Builds the home (index) page
 */
class IndexPageGenerator(posts: List<Post>, numPosts: Int = 1, postsPerPage: Int = 1) : GeneratorPipeline, AbstractPostListGenerator(posts, numPosts, postsPerPage) {

	override val TEMPLATE: String = "index"

	override suspend fun process(project: Project, renderer: Renderer, fileHandler: FileHandler) {
		info("Building index file")

		val model = mutableMapOf<String, Any>()
		model.putAll(project.model)
		// only include blog posts, not pages other other layouts
		model.put("posts", posts.filter { post -> post.layout == "post" }.sortedByDescending { it.date }.take(postsPerPage))
		model.put("postCount", numPosts)
		model.put("\$thisPage","index")

		val renderedContent = renderer.render(model, TEMPLATE)

		fileHandler.writeFile(project.dirs.output, "$TEMPLATE.html", renderedContent)
	}
}

