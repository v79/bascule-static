package org.liamjd.bascule.scanner

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.set
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.slug
import java.io.BufferedReader

class BasculeCacheImpl(val project: Project, val fileHandler: FileHandler) : BasculeCache {

	override fun writeCacheFile(mdCacheItems: Set<MDCacheItem>) {
		val json = Json(JsonConfiguration(prettyPrint = true))
		val jsonData = json.stringify(MDCacheItem.serializer().set, mdCacheItems)
		fileHandler.writeFile(project.dirs.sources,getCacheFileName(),jsonData)
	}

	override fun loadCacheFile(): Set<MDCacheItem> {
		val cacheFile = fileHandler.getFileStream(project.dirs.sources, getCacheFileName())
		val jsonString = cacheFile.bufferedReader().use(BufferedReader::readText)
		val json = Json(JsonConfiguration(prettyPrint = true))
		val cacheItems: Set<MDCacheItem> = json.parse(MDCacheItem.serializer().set, jsonString)
		return cacheItems
	}

	private fun getCacheFileName(): String {
		return "${project.name.slug()}.cache.json"
	}
}
