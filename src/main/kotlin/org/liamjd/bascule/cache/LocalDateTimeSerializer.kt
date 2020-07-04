package org.liamjd.bascule.cache

import kotlinx.serialization.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * This serializer only works to ISO_DATE_TIME; nanoseconds are lost?
 */
@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer: KSerializer<LocalDateTime> {
	override val descriptor: SerialDescriptor =
			PrimitiveDescriptor("LastModified", PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, obj: LocalDateTime) {
		encoder.encodeString(obj.format(DateTimeFormatter.ISO_DATE_TIME))
	}

	override fun deserialize(decoder: Decoder): LocalDateTime {
		return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_DATE_TIME)
	}
}
