package org.liamjd.bascule.render

import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.options.MutableDataSet
import org.koin.core.parameter.ParameterList
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.flexmark.hyde.HydeExtension
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.render.Renderer
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.scanner.MDCacheItem

class HTMLRenderer(val project: Project) : KoinComponent {

	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })
	private val renderer by inject<Renderer> { ParameterList(project) }

	// TODO: this is duplicated so move it to injections somehow
	val mdOptions = MutableDataSet()
	val mdParser: Parser
	init {
		// TODO: move this into another class? Configure externally?
		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create(), TablesExtension.create(), HydeExtension.create()))
		mdOptions.set(HtmlRenderer.GENERATE_HEADER_ID, true).set(HtmlRenderer.RENDER_HEADER_ID, true) // to give headings IDs
		mdOptions.set(HtmlRenderer.INDENT_SIZE, 2) // prettier HTML
		mdOptions.set(HydeExtension.SOURCE_FOLDER, project.dirs.sources.toString())
		mdParser = Parser.builder(mdOptions).build()
	}

	fun generateHtml(post: BasculePost, mdCacheItem: MDCacheItem, itemCount: Int ) {
		println("Rendering post ${post.sourceFileName}")
		render(project.model,post,itemCount)
	}


	// no performance improvement by making this a suspending function
	private fun render(siteModel: Map<String, Any>, basculePost: BasculePost, count: Int) {

		// DEBUGGING: post is missing...
		// tag post count is 0

		val model = mutableMapOf<String, Any?>()
		model.putAll(siteModel)
		model.putAll(basculePost.toModel())
		model.put("\$currentPage", basculePost.slug)

		// first, extract the content from the markdown
		val renderedMarkdown = renderMarkdown(basculePost.document)
		model.put("content", renderedMarkdown)

		// then, render the corresponding Handlebars template
		val templateFromYaml
				: String = basculePost.layout
		val renderedContent = renderer.render(model, templateFromYaml)
		basculePost.content = renderedMarkdown

	/*	val renderProgressBar = ProgressBar(label = "Rendering", animated = true, messageLine = basculePost.url)
		renderProgressBar.progress(count)*/

		fileHandler.createDirectories(basculePost.destinationFolder!!)
		fileHandler.writeFile(project.dirs.output.absoluteFile, basculePost.url, renderedContent)

	}

	private fun renderMarkdown(document: Document): String {
		val mdRender = HtmlRenderer.builder(mdOptions).build()

		return mdRender.render(document)
	}
}
