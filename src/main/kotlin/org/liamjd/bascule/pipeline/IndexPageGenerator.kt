package org.liamjd.bascule.pipeline

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.data.MutableDataSet
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.generators.AbstractPostListGenerator
import org.liamjd.bascule.lib.generators.GeneratorPipeline
import org.liamjd.bascule.lib.generators.SortAndFilter
import org.liamjd.bascule.lib.model.Post
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.TemplatePageRenderer
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.scanner.PostBuilder
import println.info
import java.io.File

/**
 * Builds the home (index) page
 */
class IndexPageGenerator(posts: List<Post>, numPosts: Int = 1, postsPerPage: Int = 1,private val sortAndFilter: SortAndFilter) : GeneratorPipeline, AbstractPostListGenerator(posts, numPosts, postsPerPage) {

	override val TEMPLATE: String = "index"

	override suspend fun process(project: Project, renderer: TemplatePageRenderer, fileHandler: FileHandler, clean: Boolean) {
		info("Building index file")

		val model = mutableMapOf<String, Any>()
		model.putAll(project.model)
		// only include blog posts, not pages other other layouts
		// TODO: this is assuming that no more than `postsPerPage` posts will appear on the homepage; should really be configurable
//		val postsToRender = posts.filter { post -> project.postLayouts.contains(post.layout) }.sortedByDescending { it.date }.take(postsPerPage)
		val postsToRender = sortAndFilter.sortAndFilter(project, posts).flatten().take(postsPerPage)

		val postBuilder = PostBuilder(project)

		for(p in postsToRender) {
			if(p.rawContent.isEmpty()) {
				p.rawContent = fileHandler.readFileAsString(p.sourceFileName)
				// I need the rendered HTML, not just the rawContent here
				val basculePost = postBuilder.buildPost(File(p.sourceFileName))
				if(basculePost is BasculePost) {
					p.content = renderMarkdown(basculePost.document, project.markdownOptions)
				}
			}
		}
		model["posts"] = postsToRender
		model["postCount"] = numPosts
		model["\$thisPage"] = "index"

		val renderedContent = renderer.render(model, TEMPLATE)

		fileHandler.writeFile(project.dirs.output, "$TEMPLATE.html", renderedContent)
	}

	private fun renderMarkdown(document: Document, mdOptions: MutableDataSet): String {
		val mdRender = HtmlRenderer.builder(mdOptions).build()
		return mdRender.render(document)
	}
}

