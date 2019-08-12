package org.liamjd.bascule.scanner

import mu.KotlinLogging
import org.koin.core.parameter.ParameterList
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.PostLink
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.model.Tag
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostGenError
import println.ProgressBar
import println.info
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.system.measureTimeMillis

class ChangeSetCalculator(val project: Project) : KoinComponent {

	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })
	private val postBuilder: PostBuilder by inject { parametersOf(project) }
	private val logger = KotlinLogging.logger {}

	fun calculateUncachedSet(cachedSet: Set<MDCacheItem>): Set<CacheAndPost> {
		info("Scanning ${project.dirs.sources.absolutePath} for markdown files")
		logger.info { "Scanning ${project.dirs.sources.absolutePath} for markdown files" }

		val errorMap = mutableMapOf<String, Any>()
		val allSources = mutableSetOf<CacheAndPost>()
		var markdownSourceCount = 0

		val timeTaken = measureTimeMillis {
			val markdownScannerProgressBar = ProgressBar("Reading markdown files", animated = true, asPercentage = false)

			markdownSourceCount = walkFolder(project.dirs.sources, markdownScannerProgressBar, markdownSourceCount, errorMap, allSources, cachedSet)

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

	// TODO: tidy up this recursive function
	private fun walkFolder(folder: File, markdownScannerProgressBar: ProgressBar, markdownSourceCount: Int, errorMap: MutableMap<String, Any>, allSources: MutableSet<CacheAndPost>, cachedSet: Set<MDCacheItem>): Int {
		var index = 0
		var markdownSourceCount1 = markdownSourceCount
		fileLoop@ for (mdFile in folder.listFiles()) {
			index++
			// walker starts with the current director, which we don't need
			if (mdFile.absolutePath.equals(project.dirs.sources.absolutePath)) {
				continue
			}

			if (mdFile.isDirectory) {
				walkFolder(mdFile, markdownScannerProgressBar, markdownSourceCount1, errorMap, allSources, cachedSet)
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

						// if we have a cache hit (the item is recorded correctly in the cache, then skip it
						if(!project.clean) {
							if (cacheContainsItem(mdItem, cachedSet)) {
								continue@fileLoop
							}
						}

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

		for(c in cachedSet) {
			if(c.sourceFilePath.equals(mdItem.sourceFilePath) && c.sourceModificationDate.equals(mdItem.sourceModificationDate) && c.sourceFileSize.equals(mdItem.sourceFileSize)) {
				logger.info{"Cache match found for ${mdItem.sourceFilePath}"}
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
}
