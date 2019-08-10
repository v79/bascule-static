package org.liamjd.bascule.scanner

import com.vladsch.flexmark.ext.attributes.AttributesExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.options.MutableDataSet
import mu.KotlinLogging
import org.koin.standalone.KoinComponent
import org.liamjd.bascule.flexmark.hyde.HydeExtension
import org.liamjd.bascule.lib.model.PostLink
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.model.Tag
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostGenError
import org.liamjd.bascule.slug
import println.ProgressBar
import println.info
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.system.measureTimeMillis

class ChangeSetCalculator(val project: Project) : KoinComponent {

	private val logger = KotlinLogging.logger {}
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

//		cache = BasculeCacheImpl(project,fileHandler) // TODO: use DI
	}

	fun calculateUncachedSet(): Set<CacheAndPost> {
		info("Scanning ${project.dirs.sources.absolutePath} for markdown files")
		logger.info { "Scanning ${project.dirs.sources.absolutePath} for markdown files" }

		val errorMap = mutableMapOf<String, Any>()
		val allSources = mutableSetOf<CacheAndPost>()
		var markdownSourceCount = 0

		val timeTaken = measureTimeMillis {
			val markdownScannerProgressBar = ProgressBar("Reading markdown files", animated = true, asPercentage = false)

			project.dirs.sources.walk().forEachIndexed { index, mdFile ->

				if (mdFile.parentFile.name.startsWith(".") || mdFile.parentFile.name.startsWith("__")) {
					logger.warn { "Skipping file ${mdFile.name} in draft folder '${mdFile.parentFile.name}' " }
					return@forEachIndexed // skip this one
				}

				if (mdFile.name.startsWith(".") || mdFile.name.startsWith("__")) {
					markdownScannerProgressBar.progress(index, "Skipping draft file/folder '${mdFile.name}'")
					logger.warn { "Skipping draft file '${mdFile.name}' " }
					// TODO: this isn't skipping a directory whose name begins with "__" or "."
					return@forEachIndexed // skip this one
				}

				if (mdFile.isDirectory) {
					return@forEachIndexed
				}

				if (mdFile.extension.toLowerCase() != "md") {
					logger.warn { "Skipping file ${mdFile.name} as extension does not match '.md'" }
					markdownScannerProgressBar.progress(markdownSourceCount, "Skipping file ${mdFile.name} as extension does not match '.md'")
					return@forEachIndexed
				} else {
					info("Processing file ${index} ${mdFile.name}...")
					logger.debug { "Processing file ${index} ${mdFile.name}..." }

					// construct MDCacheItem for this file, and compare it with the cache file
					val fileLastModifiedDateTimeLong = mdFile.lastModified();
					val fileLastModifiedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(fileLastModifiedDateTimeLong), TimeZone
							.getDefault().toZoneId())
					val mdItem = MDCacheItem(mdFile.length(), mdFile.absolutePath, fileLastModifiedDateTime)

					// the site model needs to know the relationship between all the pages
					// specifically the links between each ite
					// and ideally the tags too

					// now need to extract the yaml frontispiece

					val inputStream = mdFile.inputStream()
					val document = parseMarkdown(inputStream)
					val post = BasculePost.createPostFromYaml(mdFile, document, project)

					// check for errors
					when (post) {
						is PostGenError -> {
							errorMap.put(mdFile.name, post.errorMessage)
						}
						is BasculePost -> {
							val sourcePath = mdFile.parentFile.absolutePath.toString().removePrefix(project.dirs.sources.absolutePath.toString())
							mdItem.layout = post.layout
							post.url = calculateUrl(post.slug, sourcePath)
							mdItem.link = PostLink(post.title, post.url, post.date)

							post.sourceFileName = mdFile.canonicalPath
							post.destinationFolder = File(project.dirs.output, sourcePath)
							post.rawContent = mdFile.readText() // TODO: this still contains the yaml front matter :(

							allSources.add(CacheAndPost(mdItem, post))
							markdownSourceCount++
						}
					}
				}
			}

			markdownScannerProgressBar.progress(markdownSourceCount, "Cache items found for all files.")

			if (markdownSourceCount != allSources.size) {
				logger.error { "Markdown source count ($markdownSourceCount) != all sources size (${allSources.size}" }
			}


			logger.info { "Cache items found for all $markdownSourceCount files." }

			// build the set of taxonomy tags (somehow this gets it wrong)
			logger.info("Building the set of tags")
			val allTags = mutableSetOf<Tag>()
			allSources.forEach { cacheAndPost ->
				logger.info("calculateCachedSet: cacheAndPost ${cacheAndPost.mdCacheItem.link.title} has tags: ${cacheAndPost.post.tags}")
				cacheAndPost.post.tags.forEach { postTag ->
					if (allTags.contains(postTag)) {
						allTags.elementAt(allTags.indexOf(postTag)).let {
							it.postCount++
							it.hasPosts = true
							cacheAndPost.mdCacheItem.tags.add(it.label)
							postTag.hasPosts = true
							postTag.postCount = it.postCount
						}
					} else {
						allTags.add(postTag)
					}
				}
			}
			// debugging tags
			logger.info { "All tags calculated" }
		}
		info("Time taken to calculate set of ${markdownSourceCount} files: ${timeTaken}ms")
		logger.info { "Time taken to calculate set of ${markdownSourceCount} files: ${timeTaken}ms" }
		if (errorMap.isNotEmpty()) {
			logger.error { "Errors found in calculations:" }
			println.error("Errors found in calculations:")
			errorMap.forEach { t, u ->
				logger.error { "$t -> $u" }
				println.error("$t -> $u")
			}
		}
		return allSources
	}

	private fun parseMarkdown(inputStream: InputStream): Document {
		val text = inputStream.bufferedReader().readText()
		return mdParser.parse(text)
	}

	private fun calculateUrl(slug: String, sourcePath: String): String {
		val url: String = if (sourcePath.isEmpty()) {
			"$slug.html"
		} else {
			"${sourcePath.removePrefix("\\")}\\$slug.html".replace("\\", "/")
		}
		return url
	}
}
