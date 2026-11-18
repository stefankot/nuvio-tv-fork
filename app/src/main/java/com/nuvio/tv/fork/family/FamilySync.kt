package com.nuvio.tv.fork.family

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Synchronizacja ustawień rodziny (właściciel 18.11.2026; projekt: plan/raporty/synchronizacja-rodzina.md).
 * - Właściciel: „Udostępnij rodzinie” tworzy rodzinę (losowy identyfikator + klucz 256 bit + sekret właściciela) i wysyła
 *   zaszyfrowany zrzut WSZYSTKICH ustawień (FamilySnapshot). Zmiany wysyłają się same przy starcie i wyjściu z aplikacji.
 * - „Dodaj urządzenie”: kod 6 cyfr ważny 10 minut, jednorazowy; serwer dostaje tylko skrót kodu i klucz rodziny
 *   zaszyfrowany kodem.
 * - Rodzina: wpisuje kod → dostaje klucz → pobiera ustawienia → aplikacja uruchamia się ponownie. Potem przy każdym
 *   starcie pobiera nowszą wersję. Urządzenia rodziny tylko pobierają.
 * Serwer: `server/family-sync/api.php` na Zenbox (właściciel), adres [SERVER].
 */
object FamilySync {
    const val PREFS = "fork_family"
    const val SERVER = "https://nuviosync.caring.design/api.php"
    private const val TAG = "ForkFamily"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    enum class Role { NONE, OWNER, MEMBER }
    data class State(val role: Role = Role.NONE, val version: Int = 0, val message: String = "", val code: String? = null, val codeUntilMs: Long = 0)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    @Volatile private var started = false

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun role(c: Context) = runCatching { Role.valueOf(prefs(c).getString("role", "NONE")!!) }.getOrDefault(Role.NONE)
    private fun refresh(c: Context, message: String = _state.value.message) {
        _state.value = _state.value.copy(role = role(c), version = prefs(c).getInt("version", 0), message = message)
    }

    // ---------- HTTP ----------
    private fun http(method: String, query: String, body: JSONObject? = null): Pair<Int, JSONObject?> {
        val conn = URL("$SERVER?$query").openConnection() as HttpURLConnection
        conn.requestMethod = method; conn.connectTimeout = 15_000; conn.readTimeout = 60_000
        conn.setRequestProperty("Accept", "application/json")
        if (body != null) {
            conn.doOutput = true; conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        val code = conn.responseCode
        val text = runCatching { (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } }.getOrNull()
        conn.disconnect()
        return code to text?.let { runCatching { JSONObject(it) }.getOrNull() }
    }

    // ---------- Właściciel ----------
    /** Tworzy rodzinę (gdy jej nie ma) i wysyła ustawienia. */
    fun share(c: Context, onDone: (String) -> Unit = {}) = scope.launch {
        val p = prefs(c)
        if (role(c) != Role.OWNER) {
            p.edit().putString("role", Role.OWNER.name)
                .putString("id", FamilyCrypto.hex(FamilyCrypto.random(32)))
                .putString("key", FamilyCrypto.b64(FamilyCrypto.random(32)))
                .putString("owner", FamilyCrypto.b64(FamilyCrypto.random(32)))
                .putString("hash", "").putInt("version", 0).commit()
        }
        val msg = upload(c, force = true)
        refresh(c, msg); withContext(Dispatchers.Main) { onDone(msg) }
    }

    /** Wysyła zrzut, gdy się zmienił (albo [force]). Zwraca komunikat. */
    private fun upload(c: Context, force: Boolean): String {
        val p = prefs(c)
        if (role(c) != Role.OWNER) return ""
        return runCatching {
            val zip = FamilySnapshot.pack(c)
            val hash = FamilyCrypto.sha256Hex(FamilyCrypto.b64(zip))
            if (!force && hash == p.getString("hash", "")) return "Ustawienia rodziny aktualne"
            val data = FamilyCrypto.b64(FamilyCrypto.encrypt(FamilyCrypto.unb64(p.getString("key", "")!!), zip))
            val (code, json) = http("POST", "a=put", JSONObject().put("id", p.getString("id", "")).put("owner", p.getString("owner", "")).put("data", data))
            if (code != 200) return "Nie udało się wysłać ustawień (serwer: $code ${json?.optString("error").orEmpty()})"
            p.edit().putString("hash", hash).putInt("version", json?.optInt("version") ?: 0).apply()
            Log.i(TAG, "uploaded v=${json?.optInt("version")} bytes=${zip.size}")
            "Ustawienia wysłane do rodziny"
        }.getOrElse { Log.w(TAG, "upload failed: ${it.javaClass.simpleName}"); "Brak połączenia z serwerem rodziny" }
    }

    /** Kod 6 cyfr dla nowego urządzenia rodziny (10 minut). */
    fun newDeviceCode(c: Context) = scope.launch {
        val p = prefs(c)
        if (role(c) != Role.OWNER) { refresh(c, "Najpierw „Udostępnij ustawienia rodzinie”"); return@launch }
        upload(c, force = false)
        val msg = runCatching {
            repeat(3) {
                val code = FamilyCrypto.pairCode()
                val payload = JSONObject().put("id", p.getString("id", "")).put("key", p.getString("key", "")).toString().toByteArray()
                val wrapped = FamilyCrypto.b64(FamilyCrypto.encrypt(FamilyCrypto.codeKey(code), payload))
                val (status, _) = http("POST", "a=pair", JSONObject().put("id", p.getString("id", "")).put("owner", p.getString("owner", ""))
                    .put("lookup", FamilyCrypto.pairLookup(code)).put("wrapped", wrapped))
                if (status == 200) {
                    _state.value = _state.value.copy(code = code, codeUntilMs = System.currentTimeMillis() + 10 * 60_000L)
                    return@runCatching "Kod ważny 10 minut"
                }
                if (status != 409) return@runCatching "Serwer odrzucił kod ($status)"
            }
            "Spróbuj ponownie"
        }.getOrElse { "Brak połączenia z serwerem rodziny" }
        refresh(c, msg)
    }

    /** Przestaje udostępniać: kasuje dane rodziny na serwerze; urządzenia rodziny przestają dostawać zmiany. */
    fun stopSharing(c: Context) = scope.launch {
        val p = prefs(c)
        if (role(c) == Role.OWNER) runCatching { http("POST", "a=delete", JSONObject().put("id", p.getString("id", "")).put("owner", p.getString("owner", ""))) }
        p.edit().clear().commit()
        _state.value = State(message = "Udostępnianie wyłączone")
    }

    // ---------- Rodzina ----------
    /** Dołącza do rodziny kodem 6 cyfr; po pobraniu ustawień aplikacja uruchamia się ponownie. */
    fun join(activity: Activity, code: String) = scope.launch {
        val c = activity.applicationContext
        val clean = code.filter { it.isDigit() }
        if (clean.length != 6) { refresh(c, "Kod ma 6 cyfr"); return@launch }
        val msg = runCatching {
            val (status, json) = http("POST", "a=redeem", JSONObject().put("lookup", FamilyCrypto.pairLookup(clean)))
            if (status == 429) return@runCatching "Za dużo prób, spróbuj za godzinę"
            if (status != 200) return@runCatching "Zły albo wygasły kod"
            val payload = JSONObject(String(FamilyCrypto.decrypt(FamilyCrypto.codeKey(clean), FamilyCrypto.unb64(json!!.getString("wrapped")))))
            prefs(c).edit().putString("role", Role.MEMBER.name).putString("id", payload.getString("id"))
                .putString("key", payload.getString("key")).putInt("version", 0).commit()
            pull(activity, announce = true) ?: "Dołączono do rodziny"
        }.getOrElse { Log.w(TAG, "join failed: ${it.javaClass.simpleName}"); "Nie udało się dołączyć (sprawdź kod i internet)" }
        refresh(c, msg)
    }

    /** Pobiera nowszą wersję ustawień; po wgraniu uruchamia aplikację ponownie. Zwraca komunikat albo null (bez zmian). */
    private fun pull(activity: Activity, announce: Boolean): String? {
        val c = activity.applicationContext
        val p = prefs(c)
        if (role(c) != Role.MEMBER) return null
        val (status, json) = http("GET", "a=get&id=${p.getString("id", "")}&since=${p.getInt("version", 0)}")
        if (status != 200 || json == null) return if (status == 404) "Rodzina nie udostępnia już ustawień" else null
        val v = json.optInt("version"); val data = json.optString("data", "")
        if (data.isEmpty() || v == p.getInt("version", 0)) return null
        val zip = FamilyCrypto.decrypt(FamilyCrypto.unb64(p.getString("key", "")!!), FamilyCrypto.unb64(data))
        val n = FamilySnapshot.apply(c, zip)
        p.edit().putInt("version", v).commit()
        Log.i(TAG, "applied v=$v files=$n")
        restart(activity, if (announce) "Pobrano ustawienia rodziny" else "Zaktualizowano ustawienia rodziny")
        return "Pobrano ustawienia rodziny"
    }

    /** Wgrane pliki ustawień czyta dopiero nowy proces: uruchom aplikację od nowa (aplikacja jest na ekranie, więc wolno). */
    private fun restart(activity: Activity, message: String) {
        activity.runOnUiThread {
            android.widget.Toast.makeText(activity, "$message — uruchamiam ponownie", android.widget.Toast.LENGTH_LONG).show()
            val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                intent?.let { activity.startActivity(it) }
                activity.finishAffinity()
                Runtime.getRuntime().exit(0)
            }, 1_500)
        }
    }

    // ---------- Automatycznie ----------
    /** Raz na proces, gdy aplikacja jest na ekranie: właściciel wysyła zmiany, rodzina pobiera nowszą wersję. */
    fun onAppVisible(activity: Activity) {
        refresh(activity)
        if (started) return
        started = true
        scope.launch {
            when (role(activity)) {
                Role.OWNER -> upload(activity.applicationContext, force = false)
                Role.MEMBER -> runCatching { pull(activity, announce = false) }
                Role.NONE -> Unit
            }
        }
    }

    /** Wyjście z aplikacji: właściciel wysyła zmiany zrobione w tej sesji. */
    fun onAppHidden(c: Context) {
        if (role(c) == Role.OWNER) scope.launch { upload(c.applicationContext, force = false) }
    }
}
