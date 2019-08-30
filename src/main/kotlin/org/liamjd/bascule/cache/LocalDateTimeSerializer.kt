package org.liamjd.bascule.cache

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
