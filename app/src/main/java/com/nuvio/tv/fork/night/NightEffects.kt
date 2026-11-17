package com.nuvio.tv.fork.night

/** Wspólna siła z poziomu Prostego → dwa ustawienia (dźwięk, obraz) i stan ciepłej nakładki (B05-7/8). */
data class NightSettings(
    val audio: Strength,
    val picture: Strength,
    val dim: Boolean,
    val skipOnHdr: Boolean = true,
)

object NightEffects {
    /** Poziom Prosty: jedna siła dla dźwięku i obrazu, bez przyciemnienia. */
    fun fromCommon(s: Strength) = NightSettings(audio = s, picture = s, dim = false)

    /** Krycie bursztynowej nakładki dla siły (ok. 8, 14, 20%). */
    fun overlayAlpha(s: Strength): Float = when (s) { Strength.OFF -> 0f; Strength.LIGHT -> 0.08f; Strength.MEDIUM -> 0.14f; Strength.STRONG -> 0.20f }

    /** Dodatkowe przyciemnienie (czerń), gdy włączone. */
    fun dimAlpha(s: Strength, dim: Boolean): Float = if (dim && s != Strength.OFF) overlayAlpha(s) * 0.5f else 0f

    /** Stan nakładki: przy HDR/Dolby Vision (gdy opcja włączona) wyłączona; reszta według obowiązującej siły. */
    data class Overlay(val amber: Float, val dim: Float)

    fun overlay(effective: Strength, settings: NightSettings, hdr: Boolean): Overlay =
        if (effective == Strength.OFF || (hdr && settings.skipOnHdr)) Overlay(0f, 0f)
        else Overlay(overlayAlpha(settings.picture.takeIf { it != Strength.OFF } ?: effective), dimAlpha(effective, settings.dim))

    /** Płynne włączanie przez 3 s: ułamek docelowego krycia po [elapsedMs] od zmiany stanu. */
    fun ramp(elapsedMs: Long, durationMs: Long = 3_000L): Float = (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)

    /** Mapowanie siły na poziom dźwięku trybu nocnego odtwarzacza (nazwy kluczy `NightModeLevel`/`NightMode`). */
    fun audioKey(s: Strength): String = when (s) { Strength.OFF -> "off"; Strength.LIGHT -> "light"; Strength.MEDIUM -> "medium"; Strength.STRONG -> "strong" }

    /** Obowiązujący poziom dźwięku: harmonogram aktywny + ustawienie użytkownika „off” = poziom z harmonogramu; inaczej zachowuje wyższy z dwóch. */
    fun effectiveAudio(userKey: String, controllerEffective: Strength, audio: Strength): String {
        if (controllerEffective == Strength.OFF) return userKey
        val sched = audioKey(audio.takeIf { it != Strength.OFF } ?: controllerEffective)
        val order = listOf("off", "light", "medium", "strong")
        return if (order.indexOf(userKey) >= order.indexOf(sched)) userKey else sched
    }
}
