package org.liamjd.bascule.scanner

import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.koin.core.parameter.ParameterList
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.FileHandler
import org.liamjd.bascule.assets.AssetsProcessor
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.generator.Post
import org.liamjd.bascule.generator.PostGenError
import org.liamjd.bascule.render.Renderer
import println.info
import java.io.File
import java.io.InputStream
import java.io.Serializable
import kotlin.system.measureTimeMillis


// TODO: this is a sort of cache. What should it contain?
class GeneratedContent(val lastUpdated: Long, val url: String, val content: String) : Serializable

class FolderWalker(val project: ProjectStructure) : KoinComponent {

	private val assetsProcessor: AssetsProcessor
	private val renderer by inject<Renderer> { ParameterList(project) }
	private val fileHandler: FileHandler by inject(parameters = { ParameterList() })

	val mdOptions = MutableDataSet()
	val mdParser: Parser


	val OUTPUT_SUFFIX = ".html"

	init {
		println("FolderWalker initialised")
		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create()))
		mdParser = Parser.builder(mdOptions).build()

		assetsProcessor = AssetsProcessor(project.root, project.assetsDir, project.outputDir)
	}

	// I wonder if coroutines can help with this?
	fun generate(): List<Post> {

		// TODO: be less agressive with this, use some sort of caching :)
		fileHandler.emptyFolder(project.outputDir,OUTPUT_SUFFIX)
		assetsProcessor.copyStatics()

		var numPosts = 0

		info("Scanning ${project.sourceDir.absolutePath} for markdown files")

		val docCache = mutableMapOf<String, GeneratedContent>()
		val siteModel = project.model
		val postList = mutableListOf<Post>()
		val errorMap = mutableMapOf<String, Any>()

		val timeTaken = measureTimeMillis {

			project.sourceDir.walk().forEach {
				if (it.isDirectory) {
					// do something with directories?
				} else if (it.extension == "md") {
					numPosts++
					val model = mutableMapOf<String, Any>()
					model.putAll(siteModel)

					val inputStream = it.inputStream()
					val inputExtension = it.extension
					val lastModifiedTime = it.lastModified()

					val document = parseMarkdown(inputStream)

					val post = Post.createPostFromYaml(it, document, project)
					when (post) {
						is Post -> {

							model.putAll(post.toModel())

							val renderedMarkdown = renderMarkdown(document)
							model.put("content", renderedMarkdown)

							val templateFromYaml
									: String = post.layout
							val renderedContent = renderer.render(model, templateFromYaml)
							post.content = renderedMarkdown

							var url = it.nameWithoutExtension
							val slug = post.slug
							if (docCache.containsKey(slug)) {
								println.error("Duplicate slug '$slug' found!")
							}
							url = "$slug.html"
							post.url = url

							info("Generating html file $url")
							File(project.outputDir.absolutePath, url)
									.bufferedWriter().use { out ->
										out.write(renderedContent)
									}
							val gc = GeneratedContent(lastModifiedTime, slug, renderedContent)
							docCache.put(post.sourceFileName, gc)

							postList.add(post)
						}
						is PostGenError -> {
							errorMap.put(it.name, post.errorMessage)
						}
					}
				} else {
					println("skipping file ${it.name}")
				}
			}
		}
		info("${timeTaken}ms to generate $numPosts files")
		if (errorMap.isNotEmpty()) {
			println.error("\nDuring processing, the following errors were found and their files were not generated:")
			errorMap.forEach {
				println.error("${it.key}\t\t->\t${it.value}")
			}
			println.error("These pages and posts will be missing from your site until you correct the errors and re-run generate.")
		}

		return postList

//		info("Writing document cache")
	}

	private fun parseMarkdown(inputStream: InputStream): Document {
		val text = inputStream.bufferedReader().readText()
		return mdParser.parse(text)
	}

	private fun renderMarkdown(document: Document): String {
		val mdRender = HtmlRenderer.builder(mdOptions).build()
		return mdRender.render(document)
	}

}
