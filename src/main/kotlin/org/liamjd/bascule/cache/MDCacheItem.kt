package org.liamjd.bascule.cache

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.liamjd.bascule.lib.model.PostLink
import java.time.LocalDateTime

/**
 * This class is a representation of a source markdown file. Each source markdown file in the project will have a corresponding [MDCacheItem], loaded from a cache file if it exists; otherwise generated on the fly.
 * The cache item requires:
 * @param sourceFileSize the length of the markdown file, in bytes
 * @param sourceFilePath the full filename of the markdown file
 * @param sourceModificationDate the last modification date of the file, from the filesystem
 * Note that this class has an unusual override for the [MDCacheItem.equals] function; only the sourceFilePath is considered for equality
 */
@Serializable
class MDCacheItem(
    val sourceFileSize: Long,
    val sourceFilePath: String,
    @Serializable(with = LocalDateTimeSerializer::class) val sourceModificationDate: LocalDateTime
) {

    @Serializable(with = PostLinkSerializer::class)
    lateinit var link: PostLink

    val tags: MutableSet<String> = mutableSetOf()

    @Serializable(with = PostLinkSerializer::class)
    var previous: PostLink? = null

    @Serializable(with = PostLinkSerializer::class)
    var next: PostLink? = null

    var layout: String? = null

    @Transient
    var rerender = false

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("MDCacheItem: [source=$sourceFilePath], [size=$sourceFileSize], [date=${sourceModificationDate}]")
        return stringBuilder.toString()
    }

    // radical idea - hashCode is only based on the sourceFilePath! Useful for the caching sets
    override fun hashCode(): Int {
        return this.sourceFilePath.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other != null && other is MDCacheItem && this.sourceFilePath == this.sourceFilePath
    }

}

/**
 * Simple class to represent a handlebars template cache item
 */
@Serializable
class HandlebarsTemplateCacheItem(
    val layoutName: String,
    val layoutFilePath: String,
    val layoutFileSize: Long,
    @Serializable(with = LocalDateTimeSerializer::class) val layoutModificationDate: LocalDateTime
)
