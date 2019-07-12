package org.liamjd.bascule.cache

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class DocCache(val filePath: String, @Serializable(with = LocalDateTimeSerializer::class) val lastModified: LocalDateTime, val fileSize: Long)

@Serializable
data class CacheMap(val map: Map<String,DocCache>)

@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer: KSerializer<LocalDateTime> {
	override val descriptor: SerialDescriptor =
			StringDescriptor.withName("LastModified")

	override fun serialize(encoder: Encoder, obj: LocalDateTime) {
		encoder.encodeString(obj.format(DateTimeFormatter.ISO_DATE_TIME))
	}

	override fun deserialize(decoder: Decoder): LocalDateTime {
		return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_DATE_TIME)
	}
}
