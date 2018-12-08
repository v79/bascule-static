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
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.assets.AssetsProcessor
import org.liamjd.bascule.lib.model.PostLink
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.model.Tag
import org.liamjd.bascule.lib.render.Renderer
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostGenError
import println.info
import java.io.InputStream
import java.io.Serializable
import kotlin.system.measureTimeMillis


// TODO: this is a sort of cache. What should it contain?
class GeneratedContent(val lastUpdated: Long, val url: String, val content: String) : Serializable

class FolderWalker(val project: Project) : KoinComponent {

	private val assetsProcessor: AssetsProcessor
	private val renderer by inject<Renderer> { ParameterList(project) }
	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })

	val mdOptions = MutableDataSet()
	val mdParser: Parser

	init {
		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create()))
		mdParser = Parser.builder(mdOptions).build()
		assetsProcessor = AssetsProcessor(project.dirs.root, project.dirs.assets, project.dirs.output)
	}


	fun generate(): List<BasculePost> {
		info("Scanning ${project.dirs.sources.absolutePath} for markdown files")

		// TODO: copyStatics should not be here!
		assetsProcessor.copyStatics()

		var numPosts = 0
		val docCache = mutableMapOf<String, GeneratedContent>()
		val siteModel = project.model
		val errorMap = mutableMapOf<String, Any>()
		val sortedSetOfPosts = sortedSetOf<BasculePost>(comparator = BasculePost)
		val timeTaken = measureTimeMillis {
			project.dirs.sources.walk().forEach {
				if (it.name.startsWith(".") || it.name.startsWith("__")) {
					info("Skipping draft file/folder '${it.name}'")
					return@forEach // skip this one
				}
				if (it.isDirectory) {
					return@forEach
				}
				if (it.extension == "md") {
					val inputStream = it.inputStream()
					val document = parseMarkdown(inputStream)
					numPosts++

					val post = BasculePost.createPostFromYaml(it, document, project)

					when (post) {
						is BasculePost -> {
//							val gc = GeneratedContent(lastModifiedTime, slug, renderedContent)
//							docCache.put(post.sourceFileName, gc)

							// get the URL for prev/next generation. Doing this here in case there's a conflict of slugs
							val slug = post.slug
							if (docCache.containsKey(slug)) {
								println.error("Duplicate slug '$slug' found!")
							}
							val url = "$slug.html"
							post.url = url
							post.rawContent = it.readText() // TODO: this still contains the yaml front matter :(

							sortedSetOfPosts.add(post)
						}
						is PostGenError -> {
							errorMap.put(it.name, post.errorMessage)
						}
					}
				} else {
					println("skipping file '${it.name}' as it does not have the required '.md' file extension.")
				}
			}

			info("Parsed $numPosts files, ready to generate content")

			// create next/previous links
			val postList = sortedSetOfPosts.toList()
			postList.forEachIndexed { index, post ->
				if (index != 0) {
					val olderPost = postList.get(index - 1)
					post.older = PostLink(olderPost.title, olderPost.url, olderPost.date)
				}
				if (index != postList.size - 1) {
					val newerPost = postList.get(index + 1)
					post.newer = PostLink(newerPost.title, newerPost.url, newerPost.date)
				}
			}

			// build the set of taxonomy tags
			val allTags = mutableSetOf<Tag>()
			postList.forEach { post ->
				allTags.addAll(post.tags)
				post.tags.forEach { postTag ->
					if (allTags.contains(postTag)) {
						val t = allTags.find { it.equals(postTag) }
						if (t != null) {
							t.postCount++
							t.hasPosts = true
							postTag.postCount = t.postCount
						}
					}
				}
			}

			postList.forEach { post ->
				renderPost(siteModel, post)
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

		return sortedSetOfPosts.toList()

//		info("Writing document cache")
	}

	// no performance improvement by making this a suspending function
	private fun renderPost(siteModel: Map<String, Any>, basculePost: BasculePost) {
		val model = mutableMapOf<String, Any?>()
		model.putAll(siteModel)
		model.putAll(basculePost.toModel())

		// first, extract the content from the markdown
		val renderedMarkdown = renderMarkdown(basculePost.document)
		model.put("content", renderedMarkdown)

		// then, render the corresponding Handlebars template
		val templateFromYaml
				: String = basculePost.layout
		val renderedContent = renderer.render(model, templateFromYaml)
		basculePost.content = renderedMarkdown

		info("Generating html file ${basculePost.url}")
		fileHandler.writeFile(project.dirs.output.absoluteFile, basculePost.url, renderedContent)
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
