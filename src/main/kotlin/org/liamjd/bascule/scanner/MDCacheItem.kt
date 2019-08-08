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

	//@Transient
	var rerender = true

	override fun toString(): String {
		return "$link [older=${previous}] [newer=${next}]"
	}

}
