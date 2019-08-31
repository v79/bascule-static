package org.liamjd.bascule.pipeline

import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.options.MutableDataSet
import org.liamjd.bascule.flexmark.hyde.HydeExtension
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.generators.AbstractPostListGenerator
import org.liamjd.bascule.lib.generators.GeneratorPipeline
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
class IndexPageGenerator(posts: List<Post>, numPosts: Int = 1, postsPerPage: Int = 1) : GeneratorPipeline, AbstractPostListGenerator(posts, numPosts, postsPerPage) {

	private val POST_TEMPLATE = "post"
	override val TEMPLATE: String = "index"




	override suspend fun process(project: Project, renderer: TemplatePageRenderer, fileHandler: FileHandler) {
		info("Building index file")


		// TODO: ARGH, this is multiplying everywhere!
		val mdOptions = MutableDataSet()
		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create(), TablesExtension.create(), HydeExtension.create()))
		mdOptions.set(HtmlRenderer.GENERATE_HEADER_ID, true).set(HtmlRenderer.RENDER_HEADER_ID, true) // to give headings IDs
		mdOptions.set(HtmlRenderer.INDENT_SIZE, 2) // prettier HTML
		mdOptions.set(HydeExtension.SOURCE_FOLDER, project.dirs.sources.toString())



		val model = mutableMapOf<String, Any>()
		model.putAll(project.model)
		// only include blog posts, not pages other other layouts
		val postsToRender = posts.filter { post -> post.layout == POST_TEMPLATE }.sortedByDescending { it.date }.take(postsPerPage)

		val postBuilder = PostBuilder(project)

		for(p in postsToRender) {
			if(p.rawContent.isEmpty()) {
				p.rawContent = fileHandler.readFileAsString(p.sourceFileName)
				// I need the rendered HTML, not just the rawContent here
				val basculePost = postBuilder.buildPost(File(p.sourceFileName))
				if(basculePost is BasculePost) {
					p.content = renderMarkdown(basculePost.document, mdOptions)
				}
			}
		}
		model.put("posts", postsToRender)
		model.put("postCount", numPosts)
		model.put("\$thisPage","index")

		val renderedContent = renderer.render(model, TEMPLATE)

		fileHandler.writeFile(project.dirs.output, "$TEMPLATE.html", renderedContent)
	}


	fun renderMarkdown(document: Document, mdOptions: MutableDataSet): String {



		val mdRender = HtmlRenderer.builder(mdOptions).build()
		return mdRender.render(document)
	}
}

