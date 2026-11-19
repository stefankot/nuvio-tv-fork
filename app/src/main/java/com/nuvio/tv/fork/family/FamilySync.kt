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
import com.nuvio.tv.fork.logged

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
    data class State(val role: Role = Role.NONE, val version: Int = 0, val message: String = "", val code: String? = null, val codeUntilMs: Long = 0, val panelCode: Boolean = false)

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
        val text = runCatching { (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } }.logged("ForkFamily", "FamilySync.kt:58").getOrNull()
        conn.disconnect()
        return code to text?.let { runCatching { JSONObject(it) }.logged("ForkFamily", "FamilySync.kt:60").getOrNull() }
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
        if (panelNewer(c)) return "Nowe ustawienia z panelu — uruchom aplikację ponownie"
        return runCatching {
            val zip = FamilySnapshot.pack(c)
            val hash = FamilyCrypto.sha256Hex(FamilyCrypto.b64(zip))
            if (!force && hash == p.getString("hash", "")) return "Ustawienia rodziny aktualne"
            val data = FamilyCrypto.b64(FamilyCrypto.encrypt(FamilyCrypto.unb64(p.getString("key", "")!!), zip))
            val (code, json) = http("POST", "a=put", JSONObject().put("id", p.getString("id", "")).put("owner", p.getString("owner", ""))
                .put("data", data).put("epoch", p.getInt("epoch", 0)))
            if (code == 409 && json?.optString("error") == "rekey") { syncKey(c); return "Nowy klucz rodziny — wyślę przy następnej okazji" }
            if (code != 200) return "Nie udało się wysłać ustawień (serwer: $code ${json?.optString("error").orEmpty()})"
            // Baza do scalania: skróty plików, które są teraz na serwerze.
            val files = JSONObject(); FamilySnapshot.entries(zip).forEach { (rel, b) -> files.put(rel, FamilySnapshot.sha(b)) }
            p.edit().putString("hash", hash).putInt("version", json?.optInt("version") ?: 0).putString("files", files.toString()).apply()
            Log.i(TAG, "uploaded v=${json?.optInt("version")} bytes=${zip.size}")
            "Ustawienia wysłane do rodziny"
        }.getOrElse { Log.w(TAG, "upload failed: ${it.javaClass.simpleName}"); "Brak połączenia z serwerem rodziny" }
    }

    /** Właściciel: panel zapisał nowszą wersję → nie nadpisuj jej; wgraj przy najbliższym starcie aplikacji. */
    private fun panelNewer(c: Context): Boolean = runCatching {
        val p = prefs(c)
        val (status, json) = http("GET", "a=get&id=${p.getString("id", "")}&since=${p.getInt("version", 0)}")
        status == 200 && json != null && json.optInt("version") > p.getInt("version", 0) && json.optString("src") == "panel"
    }.logged("ForkFamily", "FamilySync.kt:101").getOrDefault(false)

    /** Kod 6 cyfr dla nowego urządzenia rodziny (10 minut). */
    fun newDeviceCode(c: Context) = scope.launch {
        val p = prefs(c)
        if (role(c) != Role.OWNER) { refresh(c, "Najpierw „Udostępnij ustawienia rodzinie”"); return@launch }
        upload(c, force = false)
        val msg = runCatching {
            repeat(3) {
                val code = FamilyCrypto.pairCode()
                val payload = JSONObject().put("id", p.getString("id", "")).put("key", p.getString("key", "")).put("epoch", p.getInt("epoch", 0)).toString().toByteArray()
                val wrapped = FamilyCrypto.b64(FamilyCrypto.encrypt(FamilyCrypto.codeKey(code), payload))
                val (status, _) = http("POST", "a=pair", JSONObject().put("id", p.getString("id", "")).put("owner", p.getString("owner", ""))
                    .put("lookup", FamilyCrypto.pairLookup(code)).put("wrapped", wrapped))
                if (status == 200) {
                    _state.value = _state.value.copy(code = code, codeUntilMs = System.currentTimeMillis() + 10 * 60_000L, panelCode = false)
                    return@runCatching "Kod ważny 10 minut"
                }
                if (status != 409) return@runCatching "Serwer odrzucił kod ($status)"
            }
            "Spróbuj ponownie"
        }.logged("ForkFamily", "FamilySync.kt:112").getOrElse { "Brak połączenia z serwerem rodziny" }
        refresh(c, msg)
    }

    /** Kod 6 cyfr dla panelu w przeglądarce (19.11): panel dostaje identyfikator, klucz i sekret właściciela. */
    fun newPanelCode(c: Context) = scope.launch {
        val p = prefs(c)
        if (role(c) != Role.OWNER) { refresh(c, "Najpierw „Udostępnij ustawienia rodzinie”"); return@launch }
        upload(c, force = false)
        val msg = runCatching {
            val code = FamilyCrypto.pairCode()
            val payload = JSONObject().put("id", p.getString("id", "")).put("key", p.getString("key", "")).put("owner", p.getString("owner", "")).put("epoch", p.getInt("epoch", 0)).toString().toByteArray()
            val wrapped = FamilyCrypto.b64(FamilyCrypto.encrypt(FamilyCrypto.codeKey(code), payload))
            val (status, _) = http("POST", "a=pair", JSONObject().put("id", p.getString("id", "")).put("owner", p.getString("owner", ""))
                .put("lookup", FamilyCrypto.sha256Hex("nuvio-panel:v1:$code")).put("wrapped", wrapped).put("kind", "panel"))
            if (status != 200) return@runCatching "Serwer odrzucił kod ($status)"
            _state.value = _state.value.copy(code = code, codeUntilMs = System.currentTimeMillis() + 10 * 60_000L, panelCode = true)
            "Wpisz kod w panelu: nuviosync.caring.design/panel"
        }.logged("ForkFamily", "FamilySync.kt:135").getOrElse { "Brak połączenia z serwerem rodziny" }
        refresh(c, msg)
    }

    /** Przestaje udostępniać: kasuje dane rodziny na serwerze; urządzenia rodziny przestają dostawać zmiany. */
    fun stopSharing(c: Context) = scope.launch {
        val p = prefs(c)
        if (role(c) == Role.OWNER) runCatching { http("POST", "a=delete", JSONObject().put("id", p.getString("id", "")).put("owner", p.getString("owner", ""))) }.logged("ForkFamily", "FamilySync.kt:151")
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
                .putString("key", payload.getString("key")).putInt("epoch", payload.optInt("epoch", 0)).putInt("version", 0).commit()
            pull(activity, announce = true) ?: "Dołączono do rodziny"
        }.logged("ForkFamily", "FamilySync.kt:162").getOrElse { Log.w(TAG, "join failed: ${it.javaClass.simpleName}"); "Nie udało się dołączyć (sprawdź kod i internet)" }
        refresh(c, msg)
    }

    /** Pobiera nowszą wersję ustawień; po wgraniu uruchamia aplikację ponownie. Zwraca komunikat albo null (bez zmian). */
    private fun pull(activity: Activity, announce: Boolean, now: Boolean = true): String? {
        val c = activity.applicationContext
        val p = prefs(c)
        if (role(c) == Role.NONE) return null
        if (role(c) == Role.OWNER) return if (panelNewer(c)) ownerMerge(activity, now) else null
        val (status, json) = http("GET", "a=get&id=${p.getString("id", "")}&since=${p.getInt("version", 0)}")
        if (status != 200 || json == null) return if (status == 404) "Rodzina nie udostępnia już ustawień" else null
        val v = json.optInt("version"); val data = json.optString("data", "")
        if (data.isEmpty() || v == p.getInt("version", 0)) return null
        // W trakcie pracy (co 15 min) nie restartuj: wgra się przy następnym starcie.
        if (!now) { toast(activity, "Nowe ustawienia od rodziny — uruchom aplikację ponownie"); return null }
        val zip = FamilyCrypto.decrypt(FamilyCrypto.unb64(p.getString("key", "")!!), FamilyCrypto.unb64(data))
        val n = FamilySnapshot.apply(c, zip)
        p.edit().putInt("version", v).putString("hash", FamilyCrypto.sha256Hex(FamilyCrypto.b64(zip))).commit()
        Log.i(TAG, "applied v=$v files=$n")
        restart(activity, if (announce) "Pobrano ustawienia rodziny" else "Zaktualizowano ustawienia rodziny")
        return "Pobrano ustawienia rodziny"
    }

    /**
     * Właściciel + nowsza wersja z panelu: scalanie po plikach. Plik zmieniony tylko w panelu → z panelu; zmieniony tylko
     * na telewizorze → zostaje; zmieniony w obu → nowszy wygrywa (czas zapisu w panelu vs czas zmiany pliku). Potem
     * restart (gdy coś wgrano) i wysłanie połączonego stanu.
     */
    private fun ownerMerge(activity: Activity, now: Boolean): String? {
        val c = activity.applicationContext
        val p = prefs(c)
        val (status, json) = http("GET", "a=get&id=${p.getString("id", "")}")
        if (status != 200 || json == null) return null
        val v = json.optInt("version"); val data = json.optString("data", "")
        if (data.isEmpty() || v <= p.getInt("version", 0)) return null
        if (!now) { toast(activity, "Nowe ustawienia z panelu — uruchom aplikację ponownie"); return null }
        val zip = FamilyCrypto.decrypt(FamilyCrypto.unb64(p.getString("key", "")!!), FamilyCrypto.unb64(data))
        val server = FamilySnapshot.entries(zip).mapValues { FamilySnapshot.sha(it.value) }
        val baseJson = runCatching { JSONObject(p.getString("files", "{}")!!) }.logged("ForkFamily", "FamilySync.kt:209").getOrDefault(JSONObject())
        val local = FamilySnapshot.localState(c)
        val serverMs = json.optLong("updated") * 1000
        val take = server.filter { (rel, sh) ->
            val base = baseJson.optString(rel, ""); val l = local[rel]
            sh != base && l?.first != sh && (l == null || l.first == base || serverMs > l.second)
        }.keys
        if (take.isNotEmpty()) FamilySnapshot.apply(c, zip, take)
        val files = JSONObject(); server.forEach { (rel, h) -> files.put(rel, h) }
        p.edit().putInt("version", v).putString("hash", "").putString("files", files.toString()).commit()
        Log.i(TAG, "merged v=$v from_panel=${take.size} of=${server.size}")
        if (take.isEmpty()) { upload(c, force = true); return null }
        restart(activity, "Pobrano zmiany z panelu")
        return "Pobrano zmiany z panelu"
    }

    /** Klucz urządzenia (do wymiany klucza rodziny przez panel): identyfikator + 256 bit, tylko na tym telewizorze. */
    private fun device(c: Context): Pair<String, String> {
        val p = prefs(c)
        val d = p.getString("device", null) ?: FamilyCrypto.hex(FamilyCrypto.random(16))
        val k = p.getString("dkey", null) ?: FamilyCrypto.b64(FamilyCrypto.random(32))
        if (!p.contains("device") || !p.contains("dkey")) p.edit().putString("device", d).putString("dkey", k).commit()
        return d to k
    }

    /** Panel wymienił klucz rodziny (nowa epoka) → pobierz nowy. false = ten telewizor usunięto z rodziny. */
    private fun syncKey(c: Context): Boolean {
        if (role(c) == Role.NONE) return true
        val p = prefs(c)
        val (st, json) = http("GET", "a=get&id=${p.getString("id", "")}&since=${p.getInt("version", 0)}")
        if (st != 200 || json == null || json.optInt("epoch") <= p.getInt("epoch", 0)) return true
        val (dev, dkey) = device(c)
        val (rs, rj) = http("POST", "a=rekey", JSONObject().put("id", p.getString("id", "")).put("device", dev))
        if (rs == 403) { removed(c); return false }
        if (rs != 200 || rj == null) { Log.w(TAG, "rekey failed $rs"); return true }
        val payload = JSONObject(String(FamilyCrypto.decrypt(FamilyCrypto.unb64(dkey), FamilyCrypto.unb64(rj.getString("wrap")))))
        p.edit().putString("key", payload.getString("key")).putInt("epoch", payload.getInt("epoch")).putString("hash", "").commit()
        Log.i(TAG, "rekeyed epoch=${payload.getInt("epoch")}")
        return true
    }

    /**
     * Właściciel odebrał dostęp w panelu: telewizor rodziny kasuje ustawienia od rodziny (klucze do serwisów, konto,
     * dodatki) i uruchamia się od nowa. Telewizor właściciela tylko się odłącza (to jego własne ustawienia).
     */
    private fun removed(c: Context) {
        val member = role(c) == Role.MEMBER
        prefs(c).edit().clear().commit()
        _state.value = State(message = "Ten telewizor odłączono od rodziny (zrobił to właściciel w panelu)")
        Log.i(TAG, "removed from family member=$member")
        if (!member) return
        val n = FamilySnapshot.wipe(c)
        Log.i(TAG, "wiped family settings files=$n")
        val act = lastActivity?.get()
        if (act != null && !act.isFinishing) restart(act, "Właściciel odłączył ten telewizor od rodziny, ustawienia rodziny usunięte")
        else Runtime.getRuntime().exit(0) // w tle: zamknij, żeby DataStore nie zapisał starej kopii z pamięci
    }
    @Volatile private var lastActivity: java.lang.ref.WeakReference<Activity>? = null

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
        lastActivity = java.lang.ref.WeakReference(activity)
        FamilyStats.install(activity)
        FullLog.start(activity) // właściciel 19.11: pełne logi z całego używania
        poll?.cancel()
        val first = !started
        started = true
        // Przy starcie od razu, potem co 15 minut, dopóki aplikacja jest na ekranie (panel zmienia ustawienia zdalnie).
        poll = scope.launch {
            var now = first
            while (true) {
                if (runCatching { syncKey(activity.applicationContext) }.logged("ForkFamily", "FamilySync.kt:296").getOrDefault(true)) runCatching {
                    if (role(activity) == Role.OWNER && !panelNewer(activity)) upload(activity.applicationContext, force = false)
                    else pull(activity, announce = false, now = now)
                }.logged("ForkFamily", "FamilySync.kt:296")
                report(activity.applicationContext)
                now = false
                kotlinx.coroutines.delay(15 * 60_000L)
            }
        }
    }
    @Volatile private var poll: kotlinx.coroutines.Job? = null

    private fun toast(activity: Activity, msg: String) = activity.runOnUiThread {
        android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_LONG).show()
    }

    /** Raport dla panelu: urządzenie (model, Android, wersja) i kolejka zdarzeń FamilyStats. Tylko w rodzinie. */
    private fun report(c: Context) {
        if (role(c) == Role.NONE) return
        val p = prefs(c)
        val (dev, dkey) = device(c)
        val events = FamilyStats.take(c)
        FamilyUsage.drain(c)?.let { events.put(JSONObject().put("k", "usage").put("t", System.currentTimeMillis()).put("c", it)) }
        val ok = runCatching {
            val ver = runCatching { c.packageManager.getPackageInfo(c.packageName, 0).versionName }.logged("ForkFamily", "FamilySync.kt:320").getOrNull().orEmpty()
            val info = JSONObject().put("model", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}").put("android", android.os.Build.VERSION.RELEASE)
                .put("app", ver).put("role", role(c).name.lowercase())
            // box: klucz urządzenia zaszyfrowany kluczem rodziny — tylko panel (z kluczem rodziny) go odczyta.
            val box = FamilyCrypto.b64(FamilyCrypto.encrypt(FamilyCrypto.unb64(p.getString("key", "")!!), JSONObject().put("dkey", dkey).toString().toByteArray()))
            val (st, json) = http("POST", "a=report", JSONObject().put("id", p.getString("id", "")).put("device", dev).put("info", info)
                .put("events", events).put("box", box).put("epoch", p.getInt("epoch", 0)))
            if (json?.optBoolean("removed") == true) { removed(c); return }
            st == 200
        }.logged("ForkFamily", "FamilySync.kt:319").getOrDefault(false)
        if (!ok) FamilyStats.restore(c, events)
        // Pełne logi (FullLog) do panelu: każdy niewysłany kawałek osobno; błąd = ponowna próba przy następnym raporcie.
        if (ok) FullLog.upload(c) { name, b64 ->
            http("POST", "a=logs", JSONObject().put("id", p.getString("id", "")).put("device", dev).put("name", name).put("data", b64)).first == 200
        }
    }

    /** Wyjście z aplikacji: właściciel wysyła zmiany zrobione w tej sesji. */
    fun onAppHidden(c: Context) {
        poll?.cancel(); poll = null
        if (role(c) == Role.OWNER) scope.launch { upload(c.applicationContext, force = false) }
        if (role(c) != Role.NONE) scope.launch { report(c.applicationContext) }
    }
}
