package org.liamjd.bascule.scanner

import kotlinx.serialization.Serializable
import org.liamjd.bascule.cache.LocalDateTimeSerializer
import org.liamjd.bascule.lib.model.PostLink
import java.time.LocalDateTime

@Serializable
class MDCacheItem(val sourceFileSize: Long, val sourceFilePath: String, @Serializable(with = LocalDateTimeSerializer::class) val sourceModificationDate: LocalDateTime) {

	@Serializable(with = PostLinkSerializer::class)
	lateinit var link: PostLink

	val tags: MutableSet<String> = mutableSetOf()

	@Serializable(with = PostLinkSerializer::class)
	var previous: PostLink? = null

	@Serializable(with = PostLinkSerializer::class)
	var next: PostLink? = null

	var layout: String? = null

	@Transient
	var rerender = true

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
		return other != null && other is MDCacheItem && this.sourceFilePath.equals(this.sourceFilePath)
	}

}
