package org.liamjd.bascule.scanner

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
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostStatus
import org.liamjd.bascule.slug
import java.io.File

class PostBuilder(val project: Project) : KoinComponent {

	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })

	val mdOptions = MutableDataSet()
	val mdParser: Parser
	private val cacheFileName: String

	init {
		// TODO: move this into another class? Configure externally?
		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create(), TablesExtension.create(), HydeExtension.create()))
		mdOptions.set(HtmlRenderer.GENERATE_HEADER_ID, true).set(HtmlRenderer.RENDER_HEADER_ID, true) // to give headings IDs
		mdOptions.set(HtmlRenderer.INDENT_SIZE, 2) // prettier HTML
		mdOptions.set(HydeExtension.SOURCE_FOLDER, project.dirs.sources.toString())
		mdParser = Parser.builder(mdOptions).build()
		cacheFileName = "${project.name.slug()}.cache.json"
	}

	fun buildPost(mdFile: File): PostStatus {
		val document = parseMarkdown(mdFile)
		val post = BasculePost.createPostFromYaml(mdFile, document, project)

		return post
	}

	private fun parseMarkdown(file: File): Document {
		val text = fileHandler.readFileAsString(file.parentFile, file.name)
		return mdParser.parse(text)
	}
}
