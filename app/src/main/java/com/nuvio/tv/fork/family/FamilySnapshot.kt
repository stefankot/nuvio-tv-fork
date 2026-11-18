package com.nuvio.tv.fork.family

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Zrzut WSZYSTKICH ustawień aplikacji (właściciel 18.11: także konto Nuvio i wybrany profil): magazyny ustawień
 * (`files/datastore`), preferencje (`shared_prefs`) i playlisty Live TV (`files/live_tv`, bez przewodnika).
 * Pominięte tylko to, co zależy od urządzenia albo jest pamięcią podręczną, i dane samej rodziny.
 */
internal object FamilySnapshot {
    private val dirs = listOf("files/datastore", "shared_prefs", "files/live_tv")
    /** Fragmenty nazw plików, które NIE jadą do rodziny. */
    private val skip = listOf(
        FamilySync.PREFS, // klucz i rola tego urządzenia
        "device_local", "startup_splash", "external_playback_pending", "image_performance", "thumb_connections",
        "guide", "WebView", "com.google.android.gms", "crash", "sentry", "_shadow", ".tmp", "smart_bad_sources",
    )

    private fun wanted(rel: String): Boolean = skip.none { rel.contains(it, ignoreCase = true) }

    private fun files(context: Context): List<Pair<String, File>> {
        val root = File(context.applicationInfo.dataDir)
        return dirs.flatMap { d ->
            File(root, d).walkTopDown().filter { it.isFile }.map { it.relativeTo(root).path to it }.filter { wanted(it.first) }.toList()
        }.sortedBy { it.first }
    }

    fun pack(context: Context): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((rel, f) in files(context)) {
                val e = ZipEntry(rel); e.time = 0L // stała data: ten sam stan = ten sam zrzut
                zip.putNextEntry(e); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    fun sha(b: ByteArray): String = FamilyCrypto.hex(java.security.MessageDigest.getInstance("SHA-256").digest(b))

    /**
     * Odebrany dostęp (19.11): kasuje wszystko, co przyszło od rodziny — klucze do serwisów, konto, dodatki, profile,
     * playlisty (te same pliki co w zrzucie). Po tym aplikację trzeba od razu zamknąć (DataStore trzyma kopię w pamięci).
     */
    fun wipe(context: Context): Int = files(context).count { (_, f) -> f.delete() }

    /** Pliki zrzutu: ścieżka → zawartość. */
    fun entries(zipBytes: ByteArray): Map<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            while (true) { val e = zip.nextEntry ?: break; if (!e.isDirectory) out[e.name] = zip.readBytes() }
        }
        return out
    }

    /** Stan plików na tym telewizorze: ścieżka → (skrót, czas zmiany). Do scalania z panelem. */
    fun localState(context: Context): Map<String, Pair<String, Long>> =
        files(context).associate { (rel, f) -> rel to (sha(f.readBytes()) to f.lastModified()) }

    /** Zapisuje pliki z zrzutu (przez plik tymczasowy). Ścieżki tylko w [dirs]; [only] = tylko te pliki. Zwraca liczbę plików. */
    fun apply(context: Context, zipBytes: ByteArray, only: Set<String>? = null): Int {
        val root = File(context.applicationInfo.dataDir).canonicalFile
        var n = 0
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                val rel = e.name
                if (e.isDirectory || dirs.none { rel.startsWith("$it/") } || !wanted(rel) || (only != null && rel !in only)) continue
                val target = File(root, rel).canonicalFile
                if (!target.path.startsWith(root.path + File.separator)) continue
                target.parentFile?.mkdirs()
                val tmp = File(target.parentFile, target.name + ".fam")
                tmp.outputStream().use { zip.copyTo(it) }
                if (!tmp.renameTo(target)) { target.delete(); tmp.renameTo(target) }
                n++
            }
        }
        return n
    }
}
