@file:Suppress("UnusedImport")

package org.liamjd.bascule.cache

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.slug
import java.io.FileNotFoundException

/**
 * Using kotlinx.serialization to serialize the cache items
 * The plug-in isn't working correctly in IntelliJ - do not "optimise" the imports
 */
class BasculeCacheImpl(val project: Project, val fileHandler: FileHandler) : BasculeCache {

	override fun writeCacheFile(mdCacheItems: Set<MDCacheItem>) {
		val json = Json(JsonConfiguration(prettyPrint = true))
		val jsonData = json.stringify(MDCacheItem.serializer().set, mdCacheItems)
		fileHandler.writeFile(project.dirs.sources, getCacheFileName(), jsonData)
	}

	override fun loadCacheFile(): Set<MDCacheItem> {
		try {
			val jsonString = fileHandler.readFileAsString(project.dirs.sources, getCacheFileName())
			val json = Json(JsonConfiguration(prettyPrint = true))
			val cacheItems: Set<MDCacheItem> = json.parse(MDCacheItem.serializer().set, jsonString)
			return cacheItems
		} catch (fnfe: FileNotFoundException) {
			return emptySet()
		}
	}

	private fun getCacheFileName(): String {
		return "${project.name.slug()}.cache.json"
	}
}
