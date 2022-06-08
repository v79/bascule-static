package org.liamjd.bascule.cache

import org.liamjd.bascule.model.BasculePost

// TODO: move to the library
/**
 * Interface for saving and loading [MDCacheItem]s from a cache file
 */
interface BasculeCache {

	// TODO: extend this cache to include basic information about the markdown templates
	/**
	 * Write a set of [MDCacheItem] to a cache file
	 */
	fun writeCacheFile(mdCacheItems: Set<MDCacheItem>)

	/**
	 * Load from the cache file into a set of [MDCacheItem]
	 */
	fun loadCacheFile(): Set<MDCacheItem>

	/**
	 * Load the handlebars template information, so we know if we need to trigger a clean generation regardless
	 */
	fun loadTemplates(): Set<HandlebarsTemplateCacheItem>

}

/**
 * Simple class to represent both a cache item and its corresponding rendered Post
 */
class CacheAndPost(val mdCacheItem: MDCacheItem, val post: BasculePost?)

