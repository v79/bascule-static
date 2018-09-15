package org.liamjd.bascule.scanner

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.context.FieldValueResolver
import com.github.jknack.handlebars.context.JavaBeanValueResolver
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.context.MethodValueResolver
import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet
import org.liamjd.bascule.FileHandler
import org.liamjd.bascule.assets.AssetsProcessor
import org.liamjd.bascule.assets.ProjectStructure
import org.liamjd.bascule.generator.Post
import org.liamjd.bascule.generator.PostGenError
import org.liamjd.bascule.render.ForEachHelper
import org.liamjd.bascule.render.LocalDateFormatter
import println.info
import java.io.File
import java.io.InputStream
import java.io.Serializable
import kotlin.system.measureTimeMillis


// TODO: this is a sort of cache. What should it contain?
class GeneratedContent(val lastUpdated: Long, val url: String, val content: String) : Serializable

class FolderWalker(val project: ProjectStructure) {

	private val assetsProcessor: AssetsProcessor

	val fileHandler: FileHandler = FileHandler()

	val mdOptions = MutableDataSet()
	val mdParser: Parser

	val TEMPLATE_SUFFIX = ".hbt"
	val OUTPUT_SUFFIX = ".html"

	init {
		println("FolderWalker initialised")
		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create()))
		mdParser = Parser.builder(mdOptions).build()

		assetsProcessor = AssetsProcessor(project.root, project.assetsDir, project.outputDir)
	}

	// I wonder if coroutines can help with this?
	fun generate() {

		// TODO: be less agressive with this, use some sort of caching :)
		emptyFolder(project.outputDir)
		assetsProcessor.copyStatics()

		var numPosts = 0

		info("Scanning ${project.sourceDir.absolutePath} for markdown files")

		val docCache = mutableMapOf<String,GeneratedContent>()
		val siteModel = project.model
		val postList = mutableListOf<Post>()
		val errorMap = mutableMapOf<String,Any>()

		val timeTaken = measureTimeMillis {

			project.sourceDir.walk().forEach {
				if (it.isDirectory) {
					// do something with directories?
				} else {
					numPosts++
					val model = mutableMapOf<String, Any>()
					model.putAll(siteModel)

					val inputStream = it.inputStream()
					val inputExtension = it.extension
					val lastModifiedTime = it.lastModified()

					val document = parseMarkdown(inputStream)

					val post = Post.createPostFromYaml(it,document, project)
					when(post) {
						is Post -> {

							model.putAll(post.toModel())

							val renderedMarkdown = renderMarkdown(document)
							model.put("content", renderedMarkdown)

							val templateFromYaml
									: String = post.layout
							val renderedContent = render(model, getTemplate(templateFromYaml))
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
							errorMap.put(it.name,post.errorMessage)
						}
					}
				}
			}
		}
		info("${timeTaken}ms to generate $numPosts files")
		if(errorMap.isNotEmpty()) {
			println.error("\nDuring processing, the following errors were found and their files were not generated:")
			errorMap.forEach {
				println.error("${it.key}\t\t->\t${it.value}")
			}
			println.error("These pages and posts will be missing from your site until you correct the errors and re-run generate.")
		}

		buildIndex(postList,numPosts)

//		info("Writing document cache")
	}

	fun buildIndex(posts: List<Post>, numPosts: Int = 0) {
		info("Building index file")
		val model = mutableMapOf<String,Any>()
		val postsPerPage = project.postsPerPage
		model.putAll(project.model)
		model.put("posts",posts.sortedByDescending { it.date }.take(postsPerPage))
		model.put("postCount",numPosts)
		val renderedContent = render(model, getTemplate("index"))

//		println("index -> $renderedContent")

		File(project.outputDir.absolutePath, "index.html").bufferedWriter().use { out ->
			out.write(renderedContent)
		}
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
		val matches = project.templatesDir.listFiles({ dir, name -> name == templateName + TEMPLATE_SUFFIX })

		if (matches.isNotEmpty() && matches.size == 1) {
			val found = matches[0]
			return found.readText()
		}
		println.error("ERROR - template file '$templateName' not found - unable to generate content.")
		return ""
	}

	// TODO: move this to it's own class!
	private fun render(model: Map<String, Any>, templateString: String): String {
		val hbRenderer = Handlebars()
		hbRenderer.registerHelper("forEach", ForEachHelper())
		val dateFormat = project.yamlMap["dateFormat"] as String ?: "dd/MMM/yyyy"
		hbRenderer.registerHelper("localDate", LocalDateFormatter(dateFormat))

		// ToDO: Do I register helpers like dateFormat helper here?

		val hbContext = Context.newBuilder(model).resolver(MethodValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, MapValueResolver.INSTANCE, FieldValueResolver.INSTANCE).build()
		val template = hbRenderer.compileInline(templateString)

		return template.apply(hbContext)
	}

	private fun emptyFolder(folder: File) {
		info("Clearing out old generated files")
		folder.walk().forEach {
			if (it != folder && it.name.endsWith(OUTPUT_SUFFIX)) {
				it.delete()
			}
		}
	}

}
