package org.liamjd.bascule.cache

import kotlinx.serialization.*
import org.liamjd.bascule.lib.model.PostLink
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Used by kotlinx.serialization in order to write the [PostLink] class to a Json file.
 *
 */
object PostLinkSerializer : KSerializer<PostLink> {
	@ImplicitReflectionSerializer
	override fun deserialize(decoder: Decoder): PostLink {
		val dec: CompositeDecoder = decoder.beginStructure(descriptor)
		var title: String? = null
		var url: String? = null
		var date: LocalDate? = null
		loop@ while (true) {
			when (val i = dec.decodeElementIndex(descriptor)) {
				CompositeDecoder.READ_DONE -> break@loop
				0 -> title = dec.decodeStringElement(descriptor, i)
				1 -> url = dec.decodeStringElement(descriptor, i)
				2 -> date = longToLocalDate(dec.decodeLongElement(descriptor, i))
				else -> throw SerializationException("Unknown index $i")
			}
		}
		dec.endStructure(descriptor)

		return PostLink(title
				?: throw MissingFieldException("title"), url
				?: throw MissingFieldException("url"), date
				?: throw MissingFieldException("date"))

	}

	@ImplicitReflectionSerializer
	override fun serialize(encoder: Encoder, obj: PostLink) {
		val compositeOutput = encoder.beginStructure(descriptor)
		compositeOutput.encodeStringElement(descriptor, 0, obj.title)
		compositeOutput.encodeStringElement(descriptor, 1, obj.url)
		compositeOutput.encodeLongElement(descriptor, 2, localDateToLong(obj.date))
		compositeOutput.endStructure(descriptor)
	}

	@ImplicitReflectionSerializer
	override val descriptor: SerialDescriptor = SerialDescriptor("postLink") {
		element<String>("title") // title will have index 0
		element<String>("url")
		element<String>("date")
	}

	private fun localDateToLong(date: LocalDate): Long {
		val zoneId = ZoneId.systemDefault() // or: ZoneId.of("Europe/Oslo");
		return date.atStartOfDay(zoneId).toEpochSecond()
	}

	private fun longToLocalDate(longValue: Long): LocalDate {
		val zoneId = ZoneId.systemDefault() // or: ZoneId.of("Europe/Oslo");
		return Instant.ofEpochSecond(longValue).atZone(ZoneId.systemDefault()).toLocalDate()
	}

}
