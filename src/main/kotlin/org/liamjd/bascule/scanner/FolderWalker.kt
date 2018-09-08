package org.liamjd.bascule.scanner

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.liamjd.bascule.assets.AssetsProcessor
import org.liamjd.bascule.assets.ProjectStructure
import println.info
import java.io.File
import java.io.InputStream
import kotlin.system.measureTimeMillis


class FolderWalker(val project: ProjectStructure) {

	private val assetsProcessor: AssetsProcessor
	private lateinit var yamlBuilder: ModelBuilder

	val mdOptions = MutableDataSet()
	val mdParser: Parser

	init {
		println("FolderWalker initialised")
		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create()))
		mdParser = Parser.builder(mdOptions).build()

		assetsProcessor = AssetsProcessor(project.root, project.assetsDir, project.outputDir)

	}

	// I wonder if coroutines can help with this?
	fun generate() {

		// TODO: be less agressive with this :)
		emptyFolder(project.outputDir)
		assetsProcessor.copyStatics()

		var numFiles = 0;

		info("Scanning ${project.sourceDir.absolutePath} for markdown files")

		val timeTaken = measureTimeMillis {

			project.sourceDir.walk().forEach {
				if (it.isDirectory) {
					// do something with directories?
				} else {
					numFiles++
					info("Scanning file ${it.name}")
					val model = mutableMapOf<String, Any>()
					val inputStream = it.inputStream()
					val inputFileName = it.nameWithoutExtension
					val inputExtension = it.extension

					val document = parseMarkdown(inputStream)
					yamlBuilder = ModelBuilder(document)

					model.putAll(yamlBuilder.getModel())

					val renderedMarkdown = renderMarkdown(document)
					model.put("content", renderedMarkdown)

					val templateFromYaml: String = yamlBuilder.getAttribute("layout")
					val renderedContent = render(model, getTemplate(templateFromYaml))
//					println(renderedContent)

					var url = it.nameWithoutExtension
					val slug = yamlBuilder.getAttribute("slug")
					url += ".html"

					info("Generating html file $url")
					File(project.outputDir.absolutePath, url).bufferedWriter().use { out ->
						out.write(renderedContent)
					}
				}
			}
		}
		info("${timeTaken}ms to generate ${numFiles} files")
	}

	private fun parseMarkdown(inputStream: InputStream): Document {
		val text = inputStream.bufferedReader().readText()
		return mdParser.parse(text)
	}

	private fun renderMarkdown(document: Document): String {
		val mdRender = HtmlRenderer.builder(mdOptions).build()

		return mdRender.render(document)
	}

	private fun getTemplate(templateName: String): String {
		info("Searching ${project.templatesDir} for template named ${templateName}.html")
		val matches = project.templatesDir.listFiles({ dir, name -> name.equals(templateName + ".html") })

		if (matches.isNotEmpty() && matches.size == 1) {
			val found = matches[0]
			return found.readText()
		}
		println.error("ERROR - file $templateName not found!!!!!")
		return ""
	}

	private fun render(model: Map<String, Any>, templateString: String): String {
		val hbRenderer = Handlebars()
		val hbContext = Context.newBuilder(model).build()
		val template = hbRenderer.compileInline(templateString)

		return template.apply(hbContext)
	}

	private fun emptyFolder(folder: File) {
		folder.walk().forEach {
			if (it != folder) {
				info("Deleting $it")
				it.deleteRecursively()
			}
		}
	}
}