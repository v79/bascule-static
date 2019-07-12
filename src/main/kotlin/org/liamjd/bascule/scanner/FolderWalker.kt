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
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonParsingException
import org.koin.core.parameter.ParameterList
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.cache.CacheMap
import org.liamjd.bascule.cache.DocCache
import org.liamjd.bascule.flexmark.hyde.HydeExtension
import org.liamjd.bascule.lib.model.PostLink
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.model.Tag
import org.liamjd.bascule.lib.render.Renderer
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostGenError
import println.debug
import println.info
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.system.measureTimeMillis


class FolderWalker(val project: Project) : KoinComponent {

	private val renderer by inject<Renderer> { ParameterList(project) }
	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })

	val mdOptions = MutableDataSet()
	val mdParser: Parser

	init {
		// TODO: move this into another class? Configure externally?
		mdOptions.set(Parser.EXTENSIONS, arrayListOf(AttributesExtension.create(), YamlFrontMatterExtension.create(), TablesExtension.create(), HydeExtension.create()))
		mdOptions.set(GENERATE_HEADER_ID, true).set(HtmlRenderer.RENDER_HEADER_ID, true) // to give headings IDs
		mdOptions.set(INDENT_SIZE, 2) // prettier HTML
		mdOptions.set(HydeExtension.SOURCE_FOLDER, project.dirs.sources.toString())
		mdParser = Parser.builder(mdOptions).build()
//		assetsProcessor = AssetsProcessor(project.dirs.root, project.dirs.assets, project.dirs.output)
	}


	fun generate(): List<BasculePost> {
		info("Scanning ${project.dirs.sources.absolutePath} for markdown files")


		/** document cache:
		/for each md file, write:
		file name, or slug, or combination?
		file size
		last modified date
		 **/
		val docCacheMap = loadCacheMap()


		var numPosts = 0
		var cacheHits = 0
		val siteModel = project.model
		val errorMap = mutableMapOf<String, Any>()
		val sortedSetOfPosts = sortedSetOf<BasculePost>(comparator = BasculePost)
		val timeTaken = measureTimeMillis {
			project.dirs.sources.walk().forEach { mdFile ->
				if (mdFile.name.startsWith(".") || mdFile.name.startsWith("__")) {
					info("Skipping draft file/folder '${mdFile.name}'")
					return@forEach // skip this one
				}
				if (mdFile.isDirectory) {
					return@forEach
				}
				if (mdFile.extension == "md") {
					val fileLastModifiedDateTimeLong = mdFile.lastModified();
					val fileLastModifiedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(fileLastModifiedDateTimeLong), TimeZone
							.getDefault().toZoneId())
					val fileCache = DocCache(mdFile.absolutePath, fileLastModifiedDateTime, mdFile.length())

					docCacheMap[fileCache.filePath]?.let {
						val (destination, lastModified, fileSize) = it
						if (lastModified == fileLastModifiedDateTime && fileSize == mdFile.length()) {
							println.info("********* CACHE HIT for ${mdFile.name} ************")
							// check the file actually exists
							val genFileCheck = File(project.dirs.output,destination)
							if(genFileCheck.exists()) {
								cacheHits++
								return@forEach // skip this one
							} else {
								println.error("Cache hit for ${mdFile.name} but generated file ${destination} not found. Regenerating.")
							}
						} else {
							// cache miss
							println.debug("******* CACHE MISS for ${mdFile.name} (cache value ${it} != ${fileCache})")
						}
					}


					val inputStream = mdFile.inputStream()
					val document = parseMarkdown(inputStream)
					numPosts++

					val post = BasculePost.createPostFromYaml(mdFile, document, project)

					when (post) {
						is BasculePost -> {
							// get the URL for prev/next generation. Doing this here in case there's a conflict of slugs

							post.sourceFileName = mdFile.canonicalPath
							val sourcePath = mdFile.parentFile.absolutePath.toString().removePrefix(project.dirs.sources.absolutePath.toString())
							post.destinationFolder = File(project.dirs.output, sourcePath)
							val slug = post.slug
							val url: String = if (sourcePath.isEmpty()) {
								"$slug.html"
							} else {
								"${sourcePath.removePrefix("\\")}\\$slug.html".replace("\\", "/")
							}
							post.url = url
							post.rawContent = mdFile.readText() // TODO: this still contains the yaml front matter :(

							val added = sortedSetOfPosts.add(post) // sorting by date, then by url
							if (!added) {
								println.error("Post '${post.url}' was not added to the treeset; likely cause is that it has the same date and URL as another post")
								errorMap.put(mdFile.name, "Post '${post.url}' was not added to the treeset; likely cause is that it has the same date and URL as another post")
							} else {
								docCacheMap.put(post.sourceFileName, DocCache(post.url, localDateTimeFromLong(mdFile.lastModified()), mdFile.length()))
							}
						}
						is PostGenError -> {
							errorMap.put(mdFile.name, post.errorMessage)
						}
					}
				} else {
					println.error("skipping file '${mdFile.name}' as it does not have the required '.md' file extension.")
				}
			}
			info("Cached content for $cacheHits files found; skipping generation")
			info("Parsed $numPosts files, ready to generate content (sortedSetOfPosts contains ${sortedSetOfPosts.size} files)")

			// create next/previous links
			// TODO: horrible hack to only generate next/previous "post"s.  .filter { basculePost -> basculePost.layout.equals("post")  }
			val completeList = sortedSetOfPosts.toList()
			val filteredPostList = completeList.filter { basculePost -> basculePost.layout.equals("post") }
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
						} else {
							debug("Tag $t found")
						}
					}
				}
			}
			var generated = 0
			completeList.forEach { post ->
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


		writePostCache(docCacheMap)


		return sortedSetOfPosts.toList()
	}

	// no performance improvement by making this a suspending function
	private fun render(siteModel: Map<String, Any>, basculePost: BasculePost) {
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

	private fun localDateTimeFromLong(longTime: Long): LocalDateTime {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(longTime), ZoneId.systemDefault());
	}

	private fun writePostCache(docCacheMap: MutableMap<String, DocCache>) {
		info("Writing document cache")

		val map = CacheMap(docCacheMap)
		val json = Json(JsonConfiguration.Stable)
		val jsonData = json.stringify(CacheMap.serializer(), map)

		fileHandler.writeFile(project.dirs.sources, "__buildcache.tmp", jsonData)

	}

	@UnstableDefault
	private fun loadCacheMap(): MutableMap<String, DocCache> {
		val emptyMap = mutableMapOf<String, DocCache>()
		try {
			val cacheStream: String? = fileHandler.getFileStream(project.dirs.sources, "__buildcache.tmp").bufferedReader().readText()
			if (cacheStream == null) {
				return emptyMap
			} else {
				try {
					val cacheJson = cacheStream
					if (cacheJson.isNotEmpty()) {
						val map = Json.parse(CacheMap.serializer(), cacheJson)
						return map.map as MutableMap<String, DocCache>
					}

				} catch (jpe: JsonParsingException) {
					println.error("Unable to parse document cache. Message is: ${jpe.message}")
					println.error("Proceeding with full generation.")
				}
			}
			return emptyMap
		} catch (fnfe: FileNotFoundException) {
			println.error("Cache file '__buildcache.tmp' not found; proceeding with full generation.")
		} catch (e: Exception) {
			println.error("Exception: ${e.message}. Proceeding with full generation.")
		}
		return emptyMap
	}
}


