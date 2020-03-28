package org.liamjd.bascule.scanner

import mu.KotlinLogging
import org.koin.core.parameter.ParameterList
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.cache.CacheAndPost
import org.liamjd.bascule.cache.HandlebarsTemplateCacheItem
import org.liamjd.bascule.cache.MDCacheItem
import org.liamjd.bascule.lib.model.PostLink
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.model.Tag
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostGenError
import println.ProgressBar
import println.debug
import println.info
import java.io.File
import java.io.FileFilter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * Calculates which source files need to be regenerated in order to rebuild the project.
 * It compares each markdown file with its corresponding cache item (if it exists) and works out which files have changed.
 *
 * Call [ChangeSetCalculator.calculateUncachedSet] to generate the set of items which need to be regenerated
 *
 * @param project the bascule project
 *
 */
class ChangeSetCalculator(val project: Project) : KoinComponent {

	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })
	private val postBuilder: PostBuilder by inject { parametersOf(project) }
	private val logger = KotlinLogging.logger {}

	/**
	 * Calculate which markdown source files have changed or are new relative to the cache set. Recursively walks the project sources directory to find markdown files
	 * @param cachedSet the known set of [MDCacheItem]s loaded from a cache file; may be empty but not null
	 * @param layoutSet the known set of [HandlebarsTemplateCacheItem] representing each of the handlebars templates
	 * @return a set of [CacheAndPost]
	 */
	fun calculateUncachedSet(cachedSet: Set<MDCacheItem>, layoutSet: Set<HandlebarsTemplateCacheItem>): Set<CacheAndPost> {
		info("Scanning ${project.dirs.sources.absolutePath} for markdown files")
		logger.info { "Scanning ${project.dirs.sources.absolutePath} for markdown files" }

		val errorMap = mutableMapOf<String, Any>()
		val allSources = mutableSetOf<CacheAndPost>()
		var markdownSourceCount = 0

		val timeTaken = measureTimeMillis {
			val markdownScannerProgressBar = ProgressBar("Reading markdown files", animated = true, asPercentage = false)

			/** recursively walk the source folder for files
			 * allSources is updated when a source markdown file is found
			 **/
			markdownSourceCount = walkFolder(project.dirs.sources, markdownScannerProgressBar, markdownSourceCount, errorMap, allSources, cachedSet, layoutSet)

			markdownScannerProgressBar.progress(markdownSourceCount, "Cache items found for all files.")

			if (markdownSourceCount != allSources.size) {
				logger.error { "Markdown source count ($markdownSourceCount) != all sources size (${allSources.size}" }
			}

			logger.info { "Cache items found for all $markdownSourceCount files." }

			// build the set of taxonomy tags
			logger.info("Building the set of tags")

			// I need to update the postCount and hasPosts flags for each Tag in the cacheAndPost set
			updateAllTagsInPosts(allSources)
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

	/**
	 * given:
	 * allTags [
	 * 		tag { category: "genre", value: "classical", count=0, multiple=false },
	 * 		tag { category: "genre", value: "jazz" count=0, multiple=false },
	 * 		tag { category: "composer", value: "mahler", count=0, multiple=false },
	 * 		tag { category: "composer", value: "bach", count=0, multiple=false },
	 * 		tag { category: "genre", value: "classical", count=0, multiple=false },
	 * 		tag { category: "composer", value: "bach", count=0, multiple=false }.
	 * 		tag { category: "genre", value: "bach", count=0, multiple=false } // weird one
	 * ]
	 * I want:
	 * groupedList {
	 * 		tag { category: "genre", value: "classical", count=2, multiple=true },
	 * 		tag { category: "genre", value: "jazz", count=1, multiple=false },
	 * 		tag { category: "composer", value: "mahler", count=1, multiple=false },
	 * 		tag { category: "composer", value: "bach", count=2, multiple=true },
	 * 		tag { category: "genre", value: "bach", count=1, multiple=false }
	 */
	private fun updateAllTagsInPosts(cacheAndPosts: Set<CacheAndPost>) {
		// get all the tags used in the project
		val allTags = mutableListOf<Tag>()
		for(cacheAndPost in cacheAndPosts) {
			cacheAndPost.post?.tags?.toCollection(allTags)
		}

		val groupedList = allTags.groupBy { it.category }.entries.flatMap { it.value.groupBy { it.label }.values.map { it.reduce { acc, item -> Tag(item.category, item.label,item.url, acc.postCount + item.postCount, false )}}}

		info("Updating tag counts on each post")
		for (cacheAndPost in cacheAndPosts) {
			if(cacheAndPost.post != null) {
				cacheAndPost.post.tags.forEach { tag ->
					val matchingTag = groupedList.first { (it == tag) }
					tag.postCount = matchingTag.postCount
					if(tag.postCount > 1) {
						tag.hasPosts = true
					}
				}
			}
		}
	}

	// TODO: tidy up this recursive function
	private fun walkFolder(folder: File, markdownScannerProgressBar: ProgressBar, markdownSourceCount: Int, errorMap: MutableMap<String, Any>, allSources: MutableSet<CacheAndPost>, cachedSet: Set<MDCacheItem>, layoutSet: Set<HandlebarsTemplateCacheItem>): Int {
		var index = 0
		var markdownSourceCount1 = markdownSourceCount
		fileLoop@ for (mdFile in folder.listFiles()) {
			index++
			// walker starts with the current directory, which we don't need
			if (mdFile.absolutePath.equals(project.dirs.sources.absolutePath)) {
				continue
			}

			if (mdFile.isDirectory) {
				walkFolder(mdFile, markdownScannerProgressBar, markdownSourceCount1, errorMap, allSources, cachedSet, layoutSet)
			}

			if (mdFile.parentFile.name.startsWith(".") || mdFile.parentFile.name.startsWith("__")) {
				logger.warn { "Skipping file ${mdFile.name} in draft folder '${mdFile.parentFile.name}' " }
				continue // skip this one
			}

			if (mdFile.name.startsWith(".") || mdFile.name.startsWith("__")) {
				markdownScannerProgressBar.progress(index, "Skipping draft file/folder '${mdFile.name}'")
				logger.warn { "Skipping draft file '${mdFile.name}' " }
				// TODO: this isn't skipping a directory whose name begins with "__" or "."
				continue // skip this one
			}

			if (mdFile.extension.toLowerCase() != "md") {
				logger.warn { "Skipping file ${mdFile.name} as extension does not match '.md'" }
				markdownScannerProgressBar.progress(markdownSourceCount1, "Skipping file ${mdFile.name} as extension does not match '.md'")
				continue
			} else {

				/** Finally, we have something we can parse as a BasculePost!!! **/
				val post = postBuilder.buildPost(mdFile)

				info("Processing file ${index} ${mdFile.name}...")
				logger.debug { "Processing file ${index} ${mdFile.name}..." }

				// construct MDCacheItem for this file, and compare it with the cache file
				val fileLastModifiedDateTimeLong = mdFile.lastModified();
				val fileLastModifiedDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(fileLastModifiedDateTimeLong), TimeZone
						.getDefault().toZoneId())
				val mdItem = MDCacheItem(mdFile.length(), mdFile.absolutePath, fileLastModifiedDateTime)


				// check for errors
				when (post) {
					is PostGenError -> {
						errorMap.put(mdFile.name, post.errorMessage)
					}
					is BasculePost -> {
						// if we have a cache hit (the item is recorded correctly in the cache), then skip it
						if (!project.clean) {
							if (cacheContainsItem(mdItem, cachedSet)) {
								// check the template. If it has been updated, the post must be re-rerendered regardless of the cache content
								val htTemplateFile: File = getTemplate(project.dirs.templates, post.layout)
								val templateCacheItem = layoutSet.find { it.layoutName == post.layout }
								if (templateCacheItem != null) {
									if (localDateTimeToLong(templateCacheItem.layoutModificationDate) != htTemplateFile.lastModified() / 1000) {
										info("Template '${post.layout}' has been modified; this post needs regenerated even though markdown source has not been changed since last generation.")
										// should fall to end of if statements now
									} else {
										mdItem.rerender = false // unnecessary, should be true

										// TODO:  all this duplication!
										val sourcePath = mdFile.parentFile.absolutePath.toString().removePrefix(project.dirs.sources.absolutePath.toString())
										mdItem.layout = post.layout
										post.url = calculateUrl(post.slug, sourcePath)
										mdItem.link = PostLink(post.title, post.url, post.date)

										post.sourceFileName = mdFile.canonicalPath
										post.destinationFolder = fileHandler.getFile(project.dirs.output, sourcePath)

										allSources.add(CacheAndPost(mdItem, post))
										continue@fileLoop
									}
								} else {
									println.error("Template ${post.layout} not found in template cache. Odd.")
								}

							}
						}
						// else, build the post and flag it for rerendering
						mdItem.rerender = true

						val sourcePath = mdFile.parentFile.absolutePath.toString().removePrefix(project.dirs.sources.absolutePath.toString())
						mdItem.layout = post.layout
						post.url = calculateUrl(post.slug, sourcePath)
						mdItem.link = PostLink(post.title, post.url, post.date)

						post.sourceFileName = mdFile.canonicalPath
						post.destinationFolder = fileHandler.getFile(project.dirs.output, sourcePath)
						post.rawContent = fileHandler.readFileAsString(mdFile.parentFile, mdFile.name) // TODO: this still contains the yaml front matter :(

						allSources.add(CacheAndPost(mdItem, post))
						markdownSourceCount1++
					}
				}
			}
		}
		return markdownSourceCount1
	}


	private fun cacheContainsItem(mdItem: MDCacheItem, cachedSet: Set<MDCacheItem>): Boolean {
		var cacheFound = false;

		for (c in cachedSet) {
			if (c.sourceFilePath.equals(mdItem.sourceFilePath) && c.sourceModificationDate.equals(mdItem.sourceModificationDate) && c.sourceFileSize.equals(mdItem.sourceFileSize)) {
				logger.info { "Cache match found for ${mdItem.sourceFilePath}" }
				cacheFound = true
			}
		}

		return cacheFound
	}

	private fun calculateUrl(slug: String, sourcePath: String): String {
		val url: String = if (sourcePath.isEmpty()) {
			"$slug.html"
		} else {
			"${sourcePath.removePrefix("\\")}\\$slug.html".replace("\\", "/")
		}
		return url
	}

	/**
	 * Read the existing templates from file and create HandlebarsTemplateCacheItem cache items for each of them
	 */
// TODO: move to interface
	fun getTemplates(templateDir: File): Set<HandlebarsTemplateCacheItem> {
		val templateSet = mutableSetOf<HandlebarsTemplateCacheItem>()
		val templates = templateDir.listFiles(FileFilter { it.extension.toLowerCase() == "hbs" })
		if (templates != null) {
			templates.forEach { file ->
				debug("Loading template details for ${file.name}")
				val hbCacheItem = HandlebarsTemplateCacheItem(file.name.substringBeforeLast("."), file.absolutePath, file.length(), LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), TimeZone
						.getDefault().toZoneId()))
				templateSet.add(hbCacheItem)
			}
		}
		return templateSet
	}

// TODO: move to interface
	/**
	 * Load the Handlebars template file with the given @param layoutName
	 */
	fun getTemplate(templateDir: File, layoutName: String): File {
		return fileHandler.getFile(templateDir, layoutName + ".hbs")
	}

	// TODO: duplicated function
	private fun localDateTimeToLong(date: LocalDateTime): Long {
		val zoneId = ZoneId.systemDefault() // or: ZoneId.of("Europe/Oslo");
		return date.toEpochSecond(zoneId.rules.getOffset(LocalDateTime.now()))
	}
}
