@file:Suppress("UnusedImport")

package org.liamjd.bascule.cache

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.liamjd.bascule.lib.FileHandler
import org.liamjd.bascule.lib.model.Project
import org.liamjd.bascule.slug
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.Collections.emptySet

/**
 * Using kotlinx.serialization to serialize the cache items
 * The plug-in isn't working correctly in IntelliJ - do not "optimise" the imports
 */
class BasculeCacheImpl(val project: Project, val fileHandler: FileHandler) : BasculeCache {

    override fun writeCacheFile(mdCacheItems: Set<MDCacheItem>) {

        val cache = Cache(getTemplates(project.dirs.templates), mdCacheItems)
//        val json = Json { prettyPrint = true }
        val cacheJsonData = Json.encodeToString(Cache.serializer(), cache)

        fileHandler.writeFile(project.dirs.sources, getCacheFileName(), cacheJsonData)

//		val templateJsonData = json.stringify(HandlebarsTemplateCacheItem.serializer().set,getTemplates())
//
//		println(templateJsonData)
//		val jsonData = json.stringify(MDCacheItem.serializer().set, mdCacheItems)
//		fileHandler.writeFile(project.dirs.sources, getCacheFileName(), jsonData)
    }

    override fun loadCacheFile(): Set<MDCacheItem> {
        try {
            val jsonString = fileHandler.readFileAsString(project.dirs.sources, getCacheFileName())
            val cache = Json.decodeFromString(Cache.serializer(), jsonString)
            val cacheItems: Set<MDCacheItem> = cache.items
            return cacheItems
        } catch (fnfe: FileNotFoundException) {
            return emptySet()
        }
    }

    override fun loadTemplates(): Set<HandlebarsTemplateCacheItem> {
        try {
            val jsonString = fileHandler.readFileAsString(project.dirs.sources, getCacheFileName())
            val json = Json { prettyPrint = true }
            val cache = json.decodeFromString(Cache.serializer(), jsonString)
            return cache.layouts
        } catch (fnfe: FileNotFoundException) {
            return emptySet()
        }
    }

    private fun getCacheFileName(): String {
        return "${project.name.slug()}.cache.json"
    }

    /**
     * Read the existing templates from file and create HandlebarsTemplateCacheItem cache items for each of them
     */
    // TODO: move to interface
    fun getTemplates(templateDir: File): Set<HandlebarsTemplateCacheItem> {
        val templateSet = mutableSetOf<HandlebarsTemplateCacheItem>()
        val templates = templateDir.listFiles(FileFilter { it.extension.lowercase(Locale.getDefault()) == "hbs" })
        if (templates != null) {
            templates.forEach { file ->
                println("Loading template details for ${file.name}")
                val hbCacheItem = HandlebarsTemplateCacheItem(
                    file.name.substringBeforeLast("."), file.absolutePath, file.length(), LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(file.lastModified()), TimeZone
                            .getDefault().toZoneId()
                    )
                )
                templateSet.add(hbCacheItem)
            }
        }
        return templateSet
    }


}

@Serializable
class Cache(val layouts: Set<HandlebarsTemplateCacheItem>, val items: Set<MDCacheItem>)
