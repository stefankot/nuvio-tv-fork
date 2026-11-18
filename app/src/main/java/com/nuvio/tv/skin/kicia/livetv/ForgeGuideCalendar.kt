package com.nuvio.tv.skin.kicia.livetv

import java.time.Instant
import java.time.ZoneId

/** Same local time on another calendar day; days spanning DST are not always 24 hours. */
internal fun forgeGuideDayAnchor(nowMs: Long, dayOffset: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(nowMs).atZone(zone).plusDays(dayOffset.toLong()).toInstant().toEpochMilli()
