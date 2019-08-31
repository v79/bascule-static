package org.liamjd.bascule.render

import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.koin.core.parameter.ParameterList
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.flexmark.hyde.HydeExtension
import org.liamjd.bascule.lib.model.Project

object MarkdownOptions : KoinComponent {

	private val project: Project by inject(parameters = { ParameterList() })
	val mdOptions = MutableDataSet()

	init {
		// TODO: move this into another class? Configure externally?
		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create(), TablesExtension.create(), HydeExtension.create()))
		mdOptions.set(HtmlRenderer.GENERATE_HEADER_ID, true).set(HtmlRenderer.RENDER_HEADER_ID, true) // to give headings IDs
		mdOptions.set(HtmlRenderer.INDENT_SIZE, 2) // prettier HTML
		mdOptions.set(HydeExtension.SOURCE_FOLDER, project.dirs.sources.toString())
	}

}
