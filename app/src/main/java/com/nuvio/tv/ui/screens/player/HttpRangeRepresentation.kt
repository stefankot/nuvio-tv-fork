package com.nuvio.tv.ui.screens.player

internal data class HttpRangeRepresentation(val start: Long, val endInclusive: Long, val total: Long?) {
    companion object {
        private val bytes = Regex("""bytes\s+(\d+)-(\d+)/(\d+|\*)""", RegexOption.IGNORE_CASE)

        fun parse(value: String?): HttpRangeRepresentation? {
            val (first, last, total) = value?.trim()?.let(bytes::matchEntire)?.destructured ?: return null
            val start = first.toLongOrNull() ?: return null
            val end = last.toLongOrNull() ?: return null
            val size = if (total == "*") null else total.toLongOrNull() ?: return null
            if (end < start || size != null && end >= size) return null
            return HttpRangeRepresentation(start, end, size)
        }
    }
}

internal data class HttpRangeVersion(val etag: String?, val lastModified: String?, val length: Long?) {
    fun accepts(other: HttpRangeVersion): Boolean {
        if (length != null && other.length != null && length != other.length) return false
        if (etag != null && other.etag != null) return etag == other.etag
        return lastModified == null || other.lastModified == null || lastModified == other.lastModified
    }

    companion object {
        fun from(headers: Map<String, List<String>>): HttpRangeVersion = HttpRangeVersion(
            etag = headers.firstHeader("ETag")?.takeUnless { it.startsWith("W/", ignoreCase = true) },
            lastModified = headers.firstHeader("Last-Modified"),
            length = HttpRangeRepresentation.parse(headers.firstHeader("Content-Range"))?.total,
        )
    }
}

internal fun Map<String, List<String>>.firstHeader(name: String): String? =
    entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()?.takeIf(String::isNotBlank)
