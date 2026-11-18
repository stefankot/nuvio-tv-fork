package com.nuvio.tv.core.network

/**
 * W1 (fork): zgodność kopii KE 0a87a08 (StreamSweepEngine.kt 1:1) z bazą bez zmian w plikach.
 * KE `StreamSpeedTester.getStreamContentLength` zwraca ContentProbe(length, finalUrl); baza zwraca Long.
 * `.length` na Long = sama wartość, więc linia KE `getStreamContentLength(...).length` kompiluje się na bazie.
 */
internal val Long.length: Long get() = this
