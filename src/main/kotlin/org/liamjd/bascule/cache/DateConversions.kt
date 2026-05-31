package org.liamjd.bascule.cache

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Centralised date <-> epoch-seconds conversions used by the cache. These were previously duplicated
 * (with subtly different implementations) across [PostLinkSerializer] and ChangeSetCalculator.
 */
object DateConversions {

	/** A [LocalDate] as epoch seconds at the start of its day, in the given zone (default: system zone). */
	fun localDateToEpochSeconds(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long =
		date.atStartOfDay(zoneId).toEpochSecond()

	/** Epoch seconds back to a [LocalDate], in the given zone (default: system zone). */
	fun epochSecondsToLocalDate(epochSeconds: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
		Instant.ofEpochSecond(epochSeconds).atZone(zoneId).toLocalDate()

	/**
	 * A [LocalDateTime] as epoch seconds, using the system zone's offset *at the current moment*.
	 * NOTE: this preserves the original ChangeSetCalculator behaviour; it can be off by an hour across
	 * a DST boundary because the offset is taken from `now()` rather than from [dateTime] itself.
	 */
	fun localDateTimeToEpochSeconds(dateTime: LocalDateTime, zoneId: ZoneId = ZoneId.systemDefault()): Long =
		dateTime.toEpochSecond(zoneId.rules.getOffset(LocalDateTime.now()))
}
