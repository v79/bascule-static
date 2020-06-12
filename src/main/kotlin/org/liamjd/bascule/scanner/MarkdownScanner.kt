package org.liamjd.bascule.scanner

import kotlinx.serialization.UnstableDefault
import mu.KotlinLogging
import org.koin.core.parameter.ParameterList
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.cache.BasculeCache
import org.liamjd.bascule.cache.CacheAndPost
import org.liamjd.bascule.cache.MDCacheItem
import org.liamjd.bascule.lib.model.Project
import println.info

/**
 * The primary class for calculating which files need to be rendered to build the website.
 * @param project the bascule project
 * Call [MarkdownScanner.calculateRenderSet] to get the set of posts which need to be re-rendered
 */
@UnstableDefault
class MarkdownScanner(val project: Project) : KoinComponent {

	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })
	private val cache: BasculeCache by inject<BasculeCache> { parametersOf(project, fileHandler) }
	private val logger = KotlinLogging.logger {}
	private val changeSetCalculator: ChangeSetCalculator by inject { parametersOf(project) }

	// this method is called by the Generator
	/**
	 * Build a set of CacheAndPost items
	 * This will be the complete set of all pages in the site, sorted
	 * Only those with a [MDCacheItem.rerender] flag will need to be re-rendered as HTML
	 */
	fun calculateRenderSet(): Set<CacheAndPost> {

		// this is everything we know from the cache. it might even be empty!
		val cachedSet = cache.loadCacheFile()
		val templateSet = cache.loadTemplates()

		// if there are no changes, this set could be empty - but if the cachedSet is empty, this must be full
		val uncachedSet = changeSetCalculator.calculateUncachedSet(cachedSet, templateSet)
		logger.debug { "Uncached set size: ${uncachedSet.size}" }
		val sorted = orderPosts(uncachedSet)
		logger.debug { "Sorted uncached set size: ${sorted.size}" }

		// for every item in the cached set, if a corresponding entry exists in the uncached set, flag it for rerendering

		val toBeCached = mutableSetOf<MDCacheItem>()
		// put all the existing cache items in this set, except in clean generation mode
		if(!project.clean) toBeCached.addAll(cachedSet)
		// then add all the new cache items, regardless

		sorted.forEach { cacheAndPost ->
			if(cachedSet.contains(cacheAndPost.mdCacheItem)) {
				// we need to update the cache with the latest version of this item
				logger.debug("Updating cache item ${cacheAndPost.mdCacheItem}")
				toBeCached.remove(cacheAndPost.mdCacheItem) // weirdly, this works because i've overridden MDCacheItem.equals()
				toBeCached.add(cacheAndPost.mdCacheItem)
			} else {
				toBeCached.add(cacheAndPost.mdCacheItem)
			}
		}
		cache.writeCacheFile(toBeCached)

		return uncachedSet
	}

	/**
	 * Sorts posts according to the date in the PostLink property (user provided via yaml)
	 * Then creates the navigation links between each post
	 */
	private fun orderPosts(posts: Set<CacheAndPost>): Set<CacheAndPost> {
		info("sorting")
		val sortedSet = posts.toSortedSet(compareBy({ cacheAndPost -> cacheAndPost.mdCacheItem.link.date }, { cacheAndPost -> cacheAndPost.mdCacheItem.link.url }))
		logger.info { "${sortedSet.size} markdown files sorted" }
		info("${sortedSet.size} markdown files sorted")
		info("building next and previous links")

		val filteredList = sortedSet.filter { cacheAndPost -> project.postLayouts.contains(cacheAndPost.mdCacheItem.layout) }.toList()
		filteredList.forEachIndexed { index, cacheAndPost ->
			if (index != 0) {
				val olderPost = filteredList[index - 1].mdCacheItem
				cacheAndPost.mdCacheItem.previous = olderPost.link
				cacheAndPost.post?.older = olderPost.link
			}
			if (index != filteredList.size - 1) {
				val newerPost = filteredList[index + 1].mdCacheItem
				cacheAndPost.mdCacheItem.next = newerPost.link
				cacheAndPost.post?.newer = newerPost.link
			}
		}

		info("Excluding non-(${project.postLayouts}) items leaves ${filteredList.size}")
		logger.info { "Excluding non-(${project.postLayouts}) items leaves ${filteredList.size}" }

		return sortedSet
	}
}

