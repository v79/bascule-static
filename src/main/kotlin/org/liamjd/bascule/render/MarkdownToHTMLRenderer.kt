package org.liamjd.bascule.render

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.util.ast.Document
import mu.KotlinLogging
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.model.BasculePost
import println.info

/**
 * Transforms a given [BasculePost] into the final HTML file through the Flexmark HTMLRenderer class,
 * and writes it to disc
 */
class MarkdownToHTMLRenderer(val project: Project) : MarkdownRenderer {

	private val logger = KotlinLogging.logger {}
	private val fileHandler = BasculeFileHandler()
	private val renderer = HandlebarsRenderer(project)

//	val mdParser: Parser = Parser.builder(project.markdownOptions).build()

	override fun renderHTML(post: BasculePost, itemCount: Int): Boolean {
		info("Rendering post ${post.sourceFileName}")
		logger.info { "Rendering post ${post.sourceFileName}" }
		return render(project.model, post)
	}

	// no performance improvement by making this a suspending function
	private fun render(siteModel: Map<String, Any>, basculePost: BasculePost): Boolean {

		val templateFromYaml
				: String = basculePost.layout
		if (project.postLayouts.contains(templateFromYaml)) {
			val model = mutableMapOf<String, Any?>()
			model.putAll(siteModel)
			model.putAll(basculePost.toModel())
			model.putAll(basculePost.groupTagsByCategory())
			model["\$currentPage"] = basculePost.slug

			// first, extract the content from the markdown
			val renderedMarkdown = renderMarkdown(basculePost.document)
			model.put("content", renderedMarkdown)

			// then, render the corresponding Handlebars template

			val renderedContent = renderer.render(model, templateFromYaml)
			basculePost.content = renderedMarkdown


			/*	val renderProgressBar = ProgressBar(label = "Rendering", animated = true, messageLine = basculePost.url)
		renderProgressBar.progress(count)*/

			fileHandler.createDirectories(basculePost.destinationFolder!!)
			fileHandler.writeFile(project.dirs.output.absoluteFile, basculePost.url, renderedContent)
			return true
		} else {
			println.error("Skipping post ${basculePost.title} as its specified layout '${basculePost.layout}' is not in the project layout set ${project.postLayouts}")
			println("Skipping post ${basculePost.title} as its specified layout '${basculePost.layout}' is not in the project layout set ${project.postLayouts}")
			return false
		}
	}

	override fun renderMarkdown(document: Document): String {
		val mdRender = HtmlRenderer.builder(project.markdownOptions).build()
		return mdRender.render(document)
	}
}
