package org.liamjd.bascule.scanner

import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.HtmlRenderer.GENERATE_HEADER_ID
import com.vladsch.flexmark.html.HtmlRenderer.INDENT_SIZE
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.options.MutableDataSet
import org.koin.core.parameter.ParameterList
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.flexmark.hyde.HydeExtension
import org.liamjd.bascule.lib.model.PostLink
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.model.Tag
import org.liamjd.bascule.lib.render.Renderer
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostGenError
import println.info
import java.io.File
import java.io.InputStream
import java.io.Serializable
import kotlin.system.measureTimeMillis


// TODO: this is a sort of cache. What should it contain?
class GeneratedContent(val lastUpdated: Long, val url: String, val content: String) : Serializable

class FolderWalker(val project: Project) : KoinComponent {

	private val renderer by inject<Renderer> { ParameterList(project) }
	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })

	val mdOptions = MutableDataSet()
	val mdParser: Parser

	init {
		// TODO: move this into another class? Configure externally?
		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create(), TablesExtension.create(), HydeExtension.create()))
		mdOptions.set(GENERATE_HEADER_ID,true).set(HtmlRenderer.RENDER_HEADER_ID,true) // to give headings IDs
		mdOptions.set(INDENT_SIZE,2) // prettier HTML
		mdOptions.set(HydeExtension.SOURCE_FOLDER,project.dirs.sources.toString())
		mdParser = Parser.builder(mdOptions).build()
//		assetsProcessor = AssetsProcessor(project.dirs.root, project.dirs.assets, project.dirs.output)
	}


	fun generate(): List<BasculePost> {
		info("Scanning ${project.dirs.sources.absolutePath} for markdown files")

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
							// get the URL for prev/next generation. Doing this here in case there's a conflict of slugs

							post.sourceFileName = it.canonicalPath
							val sourcePath = it.parentFile.absolutePath.toString().removePrefix(project.dirs.sources.absolutePath.toString())
							post.destinationFolder = File(project.dirs.output,sourcePath)
							val slug = post.slug
							if (docCache.containsKey(slug)) {
								println.error("Duplicate slug '$slug' found!")
							}
							val url: String = if(sourcePath.isEmpty()) {"$slug.html"} else { "${sourcePath.removePrefix("\\")}\\$slug.html".replace("\\","/") }
							post.url = url
							post.rawContent = it.readText() // TODO: this still contains the yaml front matter :(

							val added = sortedSetOfPosts.add(post) // sorting by date, then by url
							if(!added) {
								println.error("Post '${post.url}' was not added to the treeset; likely cause is that it has the same date and URL as another post")
							}
						}
						is PostGenError -> {
							errorMap.put(it.name, post.errorMessage)
						}
					}
				} else {
					println.error("skipping file '${it.name}' as it does not have the required '.md' file extension.")
				}
			}

			info("Parsed $numPosts files, ready to generate content (sortedSetOfPosts contains ${sortedSetOfPosts.size} files)")

			// create next/previous links
			// TODO: horrible hack to only generate next/previous "post"s.  .filter { basculePost -> basculePost.layout.equals("post")  }
			val completeList = sortedSetOfPosts.toList()
			val filteredPostList = completeList.filter { basculePost -> basculePost.layout.equals("post")  }
			filteredPostList.forEachIndexed { index, post ->
				if (index != 0) {
					val olderPost = filteredPostList.get(index - 1)
					post.older = PostLink(olderPost.title, olderPost.url, olderPost.date)
				}
				if (index != filteredPostList.size - 1) {
					val newerPost = filteredPostList.get(index + 1)
					post.newer = PostLink(newerPost.title, newerPost.url, newerPost.date)
				}
			}

			// build the set of taxonomy tags
			val allTags = mutableSetOf<Tag>()
			completeList.forEach { post ->
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
			var generated = 0
			completeList.forEach{ post ->
				render(siteModel, post)
				generated++
			}
			info("Rendered html files: ${generated}")

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
	private fun render(siteModel: Map<String, Any>, basculePost: BasculePost) {
		val model = mutableMapOf<String, Any?>()
		model.putAll(siteModel)
		model.putAll(basculePost.toModel())
		model.put("\$currentPage",basculePost.slug)

		// first, extract the content from the markdown
		val renderedMarkdown = renderMarkdown(basculePost.document)
		model.put("content", renderedMarkdown)

		// then, render the corresponding Handlebars template
		val templateFromYaml
				: String = basculePost.layout
		val renderedContent = renderer.render(model, templateFromYaml)
		basculePost.content = renderedMarkdown

		info("Generating html file ${basculePost.url}")
		fileHandler.createDirectories(basculePost.destinationFolder!!)
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
