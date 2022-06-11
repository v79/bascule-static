package org.liamjd.bascule.cache

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.liamjd.bascule.lib.model.PostLink
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PostLinkMissingFieldException(val fieldName: String): Exception()

/**
 * Used by kotlinx.serialization in order to write the [PostLink] class to a Json file.
 *
 */
object PostLinkSerializer : KSerializer<PostLink> {

	override fun deserialize(decoder: Decoder): PostLink {
		val dec: CompositeDecoder = decoder.beginStructure(descriptor)
		var title: String? = null
		var url: String? = null
		var date: LocalDate? = null
		loop@ while (true) {
			when (val i = dec.decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break@loop
				0 -> title = dec.decodeStringElement(descriptor, i)
				1 -> url = dec.decodeStringElement(descriptor, i)
				2 -> date = longToLocalDate(dec.decodeLongElement(descriptor, i))
				else -> throw SerializationException("Unknown index $i")
			}
		}
		dec.endStructure(descriptor)

		return PostLink(title
				?: throw PostLinkMissingFieldException("title"), url
				?: throw PostLinkMissingFieldException("url"), date
				?: throw PostLinkMissingFieldException("date"))

	}

	override fun serialize(encoder: Encoder, obj: PostLink) {
		val compositeOutput = encoder.beginStructure(descriptor)
		compositeOutput.encodeStringElement(descriptor, 0, obj.title)
		compositeOutput.encodeStringElement(descriptor, 1, obj.url)
		compositeOutput.encodeLongElement(descriptor, 2, localDateToLong(obj.date))
		compositeOutput.endStructure(descriptor)
	}

	/*@OptIn(ExperimentalSerializationApi::class)
	override val descriptor: SerialDescriptor = SerialDescriptor("postLink") {
		element<String>("title") // title will have index 0
		element<String>("url")
		element<String>("date")
	}*/
	override val descriptor: SerialDescriptor = buildClassSerialDescriptor("postLink") {
		element<String>("title")
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
