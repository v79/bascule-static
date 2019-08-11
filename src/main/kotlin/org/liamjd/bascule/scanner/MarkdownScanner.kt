package org.liamjd.bascule.scanner

import kotlinx.serialization.UnstableDefault
import mu.KotlinLogging
import org.koin.core.parameter.ParameterList
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import org.liamjd.bascule.BasculeFileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.model.BasculePost
import println.info


@UnstableDefault
class MarkdownScanner(val project: Project) : KoinComponent {

	private val fileHandler: BasculeFileHandler by inject(parameters = { ParameterList() })
	private val cache: BasculeCache by inject<BasculeCache> { parametersOf(project,fileHandler)}
	private val logger = KotlinLogging.logger {}
	private val changeSetCalculator: ChangeSetCalculator by inject { parametersOf(project)}
	private val BLOG_POST = "post"


	// this method is called by the Generator
	fun calculateRenderSet(): Set<CacheAndPost> {

		logger.debug { "Calculate render set!!!!" }

		val cachedSet = cache.loadCacheFile()

		val uncachedSet = changeSetCalculator.calculateUncachedSet(cachedSet)

		logger.debug { "Uncached set size: ${uncachedSet.size}" }
		val sorted = orderPosts(uncachedSet)

		logger.debug { "Ordered set size: ${sorted.size}" }
		// urgh. can't kotlin do this for me?
		val toBeCached = mutableSetOf<MDCacheItem>()
		sorted.forEach { cacheAndPost ->
			toBeCached.add(cacheAndPost.mdCacheItem)
		}

		cache.writeCacheFile(toBeCached)
		return sorted
	}


	/**
	 * Sorts posts according to the date in the PostLink property (user provided via yaml)
	 * Then creates the navigation links between each post (
	 */
	private fun orderPosts(posts: Set<CacheAndPost>): Set<CacheAndPost> {
		info("sorting")
		val sortedSet = posts.toSortedSet(compareBy({ cacheAndPost -> cacheAndPost.mdCacheItem.link.date }, { cacheAndPost -> cacheAndPost.mdCacheItem.link.url }))
		logger.info { "${sortedSet.size} markdown files sorted" }
		info("${sortedSet.size} markdown files sorted")
		info("building next and previous links")

		val filteredList = sortedSet.filter { cacheAndPost -> cacheAndPost.mdCacheItem.layout.equals(BLOG_POST) }.toList()
		filteredList.forEachIndexed { index, cacheAndPost ->
			if (index != 0) {
				val olderPost = filteredList.get(index - 1).mdCacheItem
				cacheAndPost.mdCacheItem.previous = olderPost.link
				cacheAndPost.post.older = olderPost.link
			}
			if (index != filteredList.size - 1) {
				val newerPost = filteredList.get(index + 1).mdCacheItem
				cacheAndPost.mdCacheItem.next = newerPost.link
				cacheAndPost.post.newer = newerPost.link
			}
		}

		info("Excluding non-post items leaves ${filteredList.size}")
		logger.info { "Excluding non-post items leaves ${filteredList.size}" }

		return sortedSet
	}
}

class CacheAndPost(val mdCacheItem: MDCacheItem, val post: BasculePost)
