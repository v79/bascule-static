package org.liamjd.bascule.scanner

// TODO: move to the library
interface BasculeCache {


	fun writeCacheFile(mdCacheItems: Set<MDCacheItem>)

	fun loadCacheFile(): Set<MDCacheItem>

}

