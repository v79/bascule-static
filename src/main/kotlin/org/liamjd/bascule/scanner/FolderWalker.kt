package org.liamjd.bascule.scanner

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import java.io.File
import java.io.InputStream
import kotlin.system.measureTimeMillis


class FolderWalker(val parent: File, val sources: String, val templates: String, val output: String, val assets: String) {

	private val sourcesDir: File
	private val templatesDir: File
	private val outputDir: File
	private val assetsDir: File

	val mdOptions = MutableDataSet()
	val mdParser: Parser

	init {
		println("FolderWalker initialised")
		sourcesDir = File(parent, sources)
		templatesDir = File(parent, templates)
		outputDir = File(parent, output)
		assetsDir = File(parent, assets)

		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create()))
		mdParser = Parser.builder(mdOptions).build()
	}

	// I wonder if coroutines can help with this?
	fun generate() {
		var numFiles = 0;

		println("Scanning ${sourcesDir.absolutePath} for markdown files")

		val timeTaken = measureTimeMillis {

			sourcesDir.walk().forEach {
				if (it.isDirectory) {
					// do something with directories?
				} else {
					numFiles++
					println("Scanning file ${it.name}")
					val model = mutableMapOf<String, Any>()
					val inputStream = it.inputStream()
					val inputFileName = it.nameWithoutExtension
					val inputExtension = it.extension

					val document = parseMarkdown(inputStream)

					val yamlVisitor = AbstractYamlFrontMatterVisitor()
					yamlVisitor.visit(document)
					yamlVisitor.data.forEach {
						model.put(it.key, it.value[0])
					}
					println("\t model (sans content) is $model")


					val renderedMarkdown = renderMarkdown(document)
					model.put("content",renderedMarkdown)


					var templateFromYaml: String = ""
					yamlVisitor.data["layout"]?.let {
						templateFromYaml = it.get(0)
					}

					val renderedContent = render(model, getTemplate(templateFromYaml))
					println(renderedContent)
				}
			}
		}
		println("${timeTaken}ms to generate ${numFiles} files")
	}

	private fun parseMarkdown(inputStream: InputStream): Document {
		val text = inputStream.bufferedReader().readText()
		return mdParser.parse(text)
	}

	private fun renderMarkdown(document: Document): String {
		val options = MutableDataSet()
		options.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create()))
		val mdRender = HtmlRenderer.builder(options).build()

		return mdRender.render(document)
	}

	private fun getTemplate(templateName: String): String {

		println("Searching $templatesDir for template named ${templateName}.html")
		val matches = templatesDir.listFiles({ dir, name -> name.equals(templateName + ".html") })

		if (matches.isNotEmpty() && matches.size == 1) {
			val found = matches[0]
			return found.readText()
		}
		println("ERROR - file $templateName not found!!!!!")
		return ""
	}

	private fun render(model: Map<String, Any>, templateString: String): String {
		val hbRenderer = Handlebars()
		val hbContext = Context.newBuilder(model).build()
		val template = hbRenderer.compileInline(templateString)

		return template.apply(hbContext)
	}
}