package com.nuvio.tv.fork.tv.plus
// Kopia 1:1 z homelessbrian/NuvioPlusIPTV @ e5643387f7029374f7deffe1618a544b3a4396a3, app/src/main/java/com/nuvio/tv/livetv/data/ProgrammeIdentityPool.kt (zmienione: pakiet, importy modeli, PlusShims). UPSTREAM_SOURCES.md: „Silnik przewodnika NuvioPlusIPTV”.

/** A small, read-local table: guide rows share provenance text without retaining it globally. */
internal class ProgrammeIdentityPool {
    private val values = HashMap<String, String>()

    fun share(value: String?): String? {
        if (value == null || value.length > 128) return value
        values[value]?.let { return it }
        if (values.size < 128) values[value] = value
        return value
    }
}
