package org.liamjd.bascule.scanner

import mu.KotlinLogging
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.FileScanner
import org.liamjd.bascule.cache.CacheAndPost
import org.liamjd.bascule.cache.DateConversions
import org.liamjd.bascule.cache.HandlebarsTemplateCacheItem
import org.liamjd.bascule.cache.MDCacheItem
import org.liamjd.bascule.lib.model.PostLink
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.lib.model.Tag
import org.liamjd.bascule.model.BasculePost
import org.liamjd.bascule.model.PostGenError
import println.clearProgress
import println.debug
import println.info
import println.progress
import println.warn
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
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
class ChangeSetCalculator(
    val project: Project,
    private val fileHandler: BasculeFileHandler,
    private val postBuilder: PostBuilder,
    private val fileScanner: FileScanner
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Calculate which markdown source files have changed or are new relative to the cache set. Recursively walks the project sources directory to find markdown files
     * @param cachedSet the known set of [MDCacheItem]s loaded from a cache file; may be empty but not null
     * @param layoutSet the known set of [HandlebarsTemplateCacheItem] representing each of the handlebars templates
     * @return a set of [CacheAndPost]
     */
    fun calculateUncachedSet(
        cachedSet: Set<MDCacheItem>,
        layoutSet: Set<HandlebarsTemplateCacheItem>
    ): Set<CacheAndPost> {
        info("Scanning ${project.dirs.sources.absolutePath} for markdown files")
        logger.debug { "ChangeSetCalculator.calculateUncachedSet" }

        val errorMap = mutableMapOf<String, Any>()
        val allSources = mutableSetOf<CacheAndPost>()
        var markdownSourceCount = 0

        val timeTaken = measureTimeMillis {
            /** recursively walk the source folder for files
             * allSources is updated when a source markdown file is found
             **/
            markdownSourceCount = walkFolder(
                project.dirs.sources,
                markdownSourceCount,
                errorMap,
                allSources,
                cachedSet,
                layoutSet
            )

            clearProgress()
            val toRender = markdownSourceCount
            val totalFiles = allSources.size
            val cachedCount = totalFiles - toRender
            if (cachedCount > 0) {
                info("Read $totalFiles markdown files: $toRender to render, $cachedCount cached.")
            } else {
                info("Read $totalFiles markdown files.")
            }

            // build the set of taxonomy tags
            debug("Building the set of tags")
            val allTags = mutableSetOf<Tag>()
            allSources.forEach { cacheAndPost ->
                logger.debug { "ChangeSetCalculator: cacheAndPost ${cacheAndPost.mdCacheItem.link.title} has tags: ${cacheAndPost.post?.tags}" }
                cacheAndPost.post?.tags?.forEach { postTag ->
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
            debug("All tags calculated")
        }

        debug("Time taken to calculate set of ${allSources.size} files: ${timeTaken}ms")

        if (errorMap.isNotEmpty()) {
            println.error("Errors found in calculations:")
            errorMap.forEach { t, u ->
                logger.error { "$t -> $u" }
                println.error("$t -> $u")
            }
        }
        return allSources
    }

    // TODO: tidy up this recursive function
    private fun walkFolder(
        folder: File,
        markdownSourceCount: Int,
        errorMap: MutableMap<String, Any>,
        allSources: MutableSet<CacheAndPost>,
        cachedSet: Set<MDCacheItem>,
        layoutSet: Set<HandlebarsTemplateCacheItem>
    ): Int {
        var index = 0
        var markdownSourceCount1 = markdownSourceCount
        fileLoop@ for (mdFile in fileScanner.listFiles(folder)) {
            index++
            // walker starts with the current directory, which we don't need
            if (mdFile.absolutePath.equals(project.dirs.sources.absolutePath)) {
                continue
            }

            if (fileScanner.isDirectory(mdFile)) {
                markdownSourceCount1 = walkFolder(
                    mdFile,
                    markdownSourceCount1,
                    errorMap,
                    allSources,
                    cachedSet,
                    layoutSet
                )
                continue@fileLoop
            }

            if (isDraftName(mdFile.parentFile.name)) {
                logger.warn { "Skipping file ${mdFile.name} in draft folder '${mdFile.parentFile.name}' " }
                continue // skip this one
            }

            if (isDraftName(mdFile.name)) {
                warn("Skipping draft file/folder '${mdFile.name}'")
                // TODO: this isn't skipping a directory whose name begins with "__" or "."
                continue // skip this one
            }

            if (!isMarkdownFile(mdFile)) {
                logger.warn { "Skipping file ${mdFile.name} as extension does not match '.md'" }
                continue
            } else {

                /** Finally, we have something we can parse as a BasculePost!!! **/
                val post = postBuilder.buildPost(mdFile)

                progress("Reading markdown files", markdownSourceCount1, mdFile.name)
                logger.debug { "Processing file $index ${mdFile.name}..." }

                // construct MDCacheItem for this file, and compare it with the cache file
                val fileLastModifiedDateTimeLong = fileScanner.lastModified(mdFile);
                val fileLastModifiedDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(fileLastModifiedDateTimeLong), TimeZone
                        .getDefault().toZoneId()
                )
                val mdItem = MDCacheItem(fileScanner.length(mdFile), mdFile.absolutePath, fileLastModifiedDateTime)


                // check for errors
                when (post) {
                    is PostGenError -> {
                        errorMap[mdFile.name] = post.errorMessage
                    }

                    is BasculePost -> {
                        // if we have a cache hit (the item is recorded correctly in the cache), then skip it
                        if (!project.clean) {
                            if (cacheContainsItem(mdItem, cachedSet)) {
                                // check the template. If it has been updated, the post must be re-rerendered regardless of the cache content
                                val htTemplateFile: File = getTemplate(project.dirs.templates, post.layout)
                                val templateCacheItem = layoutSet.find { it.layoutName == post.layout }

                                if (templateCacheItem != null) {
                                    if (DateConversions.localDateTimeToEpochSeconds(templateCacheItem.layoutModificationDate) != fileScanner.lastModified(
                                            htTemplateFile
                                        ) / 1000
                                    ) {
                                        info("Template '${post.layout}' has been modified; this post needs regenerated even though markdown source has not been changed since last generation.")
                                        // should fall to end of if statements now
                                    } else {
                                        mdItem.rerender = false // unnecessary, should be true

                                        // TODO:  all this duplication!
                                        val sourcePath = mdFile.parentFile.absolutePath.toString()
                                            .removePrefix(project.dirs.sources.absolutePath.toString())
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

                        val sourcePath = mdFile.parentFile.absolutePath.toString()
                            .removePrefix(project.dirs.sources.absolutePath.toString())
                        mdItem.layout = post.layout
                        post.url = calculateUrl(post.slug, sourcePath)
                        mdItem.link = PostLink(post.title, post.url, post.date)

                        post.sourceFileName = mdFile.canonicalPath
                        post.destinationFolder = fileHandler.getFile(project.dirs.output, sourcePath)
                        post.rawContent = fileHandler.readFileAsString(
                            mdFile.parentFile,
                            mdFile.name
                        ) // TODO: this still contains the yaml front matter :(

                        allSources.add(CacheAndPost(mdItem, post))
                        markdownSourceCount1++
                    }
                }
            }
        }
        return markdownSourceCount1
    }


    // TODO: move to interface
    /**
     * Load the Handlebars template file with the given @param layoutName
     */
    private fun getTemplate(templateDir: File, layoutName: String): File {
        return fileHandler.getFile(templateDir, "$layoutName.hbs")
    }
}
