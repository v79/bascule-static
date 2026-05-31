package org.liamjd.bascule.cache

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class DateConversionsTest {

	// Use a fixed UTC zone so the assertions are independent of the machine's timezone.
	private val utc = ZoneId.of("UTC")

	@Test
	fun `localDate round-trips through epoch seconds`() {
		val date = LocalDate.of(2026, 5, 31)
		val epoch = DateConversions.localDateToEpochSeconds(date, utc)
		assertEquals(date, DateConversions.epochSecondsToLocalDate(epoch, utc))
	}

	@Test
	fun `localDate converts to start-of-day epoch seconds in UTC`() {
		val date = LocalDate.of(1970, 1, 2)
		// one day after the epoch = 86400 seconds
		assertEquals(86_400L, DateConversions.localDateToEpochSeconds(date, utc))
	}

	@Test
	fun `epoch seconds within a day map back to that day`() {
		// 86400 = start of 1970-01-02; add a few hours, still the same date
		val date = DateConversions.epochSecondsToLocalDate(86_400L + 3_600L, utc)
		assertEquals(LocalDate.of(1970, 1, 2), date)
	}

	@Test
	fun `localDateTime converts to epoch seconds using the supplied zone`() {
		val dateTime = LocalDateTime.of(1970, 1, 1, 1, 0, 0) // 01:00 UTC
		assertEquals(3_600L, DateConversions.localDateTimeToEpochSeconds(dateTime, ZoneOffset.UTC))
	}
}
