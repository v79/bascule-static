package org.liamjd.bascule.cache

import kotlinx.serialization.json.Json
import org.liamjd.bascule.lib.model.PostLink
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Round-trip tests for the custom kotlinx.serialization serializers used by the document cache.
 */
class CacheSerializationTest {

	@Test
	fun `LocalDateTime survives a serialize-deserialize round trip`() {
		val original = LocalDateTime.of(2026, 5, 31, 14, 30, 15)

		val json = Json.encodeToString(LocalDateTimeSerializer, original)
		val restored = Json.decodeFromString(LocalDateTimeSerializer, json)

		assertEquals(original, restored)
	}

	@Test
	fun `LocalDateTime is serialized as a quoted ISO string`() {
		val original = LocalDateTime.of(2026, 5, 31, 14, 30, 15)

		val json = Json.encodeToString(LocalDateTimeSerializer, original)

		assertEquals("\"2026-05-31T14:30:15\"", json)
	}

	@Test
	fun `PostLink title and url survive a serialize-deserialize round trip`() {
		val original = PostLink("My First Post", "/my-first-post", LocalDate.of(2026, 5, 31))

		val json = Json.encodeToString(PostLinkSerializer, original)
		val restored = Json.decodeFromString(PostLinkSerializer, json)

		assertEquals(original.title, restored.title)
		assertEquals(original.url, restored.url)
	}

	@Test
	fun `PostLink date survives a serialize-deserialize round trip`() {
		val original = PostLink("Title", "/url", LocalDate.of(2026, 5, 31))

		val json = Json.encodeToString(PostLinkSerializer, original)
		val restored = Json.decodeFromString(PostLinkSerializer, json)

		assertEquals(original.date, restored.date)
	}
}
