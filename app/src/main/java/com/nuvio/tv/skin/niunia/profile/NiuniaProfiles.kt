package com.nuvio.tv.skin.niunia.profile

import com.nuvio.tv.skin.niunia.components.niuniaTrapArrows

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.UserProfile
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import com.nuvio.tv.ui.screens.profile.ProfileSelectionViewModel

private const val TAG = "ForkNiunia"

/**
 * Przełącznik profili Niunii (canvas: Profiles, klasa .pcard): u góry po lewej pigułka „Zmień profil”, pod nią szklana karta
 * z awatarami 80 dp (fokus: powiększenie 1,08 i cień, nazwa biała), zielony znacznik przy bieżącym profilu, na końcu „Dodaj profil”.
 * Profil z PIN-em otwiera okno PIN. Weryfikacja PIN-u i wybór profilu = view model Nuvio; tu tylko wygląd i klawisze.
 */
@Composable
fun NiuniaProfiles(
    viewModel: ProfileSelectionViewModel,
    onAddProfile: () -> Unit,
    onProfileSelected: () -> Unit,
    onProfileClicked: () -> Unit,
    onProfileSelectionFailed: () -> Unit,
    onProfileFocusChanged: ((String?, String?, String?) -> Unit)?,
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val activeId by viewModel.activeProfileId.collectAsState()
    val pinEnabled by viewModel.profilePinEnabled.collectAsState()
    val working by viewModel.isPinOperationInProgress.collectAsState()
    val avatarCatalog by viewModel.avatarCatalog.collectAsState()
    val avatarUrls = remember(avatarCatalog, profiles) {
        com.nuvio.tv.skin.kicia.profile.forgeAvatarUrls(profiles, avatarCatalog.associate { it.id to it.imageUrl }, viewModel::getAvatarImageUrl)
    }
    var pinFor by remember { mutableStateOf<UserProfile?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }
    val first = remember { FocusRequester() }
    val canAdd = viewModel.canAddProfile

    LaunchedEffect(profiles.size) {
        Log.i(TAG, "profile: ${profiles.size} (aktywny id=$activeId, z PIN-em=${pinEnabled.count { it.value }}, dodawanie=${if (canAdd) "tak" else "nie"})")
        if (profiles.isNotEmpty() || canAdd) {
            try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "profile: fokus startowy: ${e.javaClass.simpleName}: ${e.message}") }
        }
    }

    fun select(profile: UserProfile) {
        Log.i(TAG, "profile: wybrano id=${profile.id}")
        NiuniaProfileSwitch.fromMenu = false
        onProfileClicked()
        viewModel.selectProfile(profile.id, onComplete = onProfileSelected, onFailure = {
            Log.w(TAG, "profile: wybór id=${profile.id} nieudany")
            onProfileSelectionFailed()
        })
    }

    // Test JMGO build151: Wstecz po „Zmień profil” z menu wychodził z aplikacji (ekran wyboru jest ekranem startowym Nuvio).
    // Otwarty z menu → Wstecz wraca do bieżącego profilu (bez PIN-u); na starcie aplikacji zostaje zachowanie Nuvio.
    val current = profiles.firstOrNull { it.id == activeId }
    BackHandler(enabled = NiuniaProfileSwitch.fromMenu && pinFor == null && current != null && pinEnabled[current.id] != true) {
        Log.i(TAG, "profile: Wstecz → bieżący profil")
        NiuniaProfileSwitch.fromMenu = false
        current?.let { select(it) }
    }

    Box(Modifier.fillMaxSize().background(com.nuvio.tv.skin.niunia.tokens.NiuniaColor.Bg)) {
        // Makieta Profiles: pigułka „‹ (profil) Zmień profil” (x 20, y 24), pod nią nakładka `profiles` (x 20, y 72): szkło
        // (Android 11 bez rozmycia: `panel`), promień 28, karty ProfileCard 72 dp co 12; bieżący profil z ✓ i fokusem startowym.
        com.nuvio.tv.skin.niunia.components.NiuniaTabPill(stringResource(R.string.niunia_profiles_title), "profile",
            Modifier.padding(start = com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space20, top = com.nuvio.tv.skin.niunia.tokens.NiuniaSize.NavTop))
        val shape = RoundedCornerShape(com.nuvio.tv.skin.niunia.tokens.NiuniaRadius.Radius28)
        Row(
            Modifier
                .padding(start = com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space20, top = com.nuvio.tv.skin.niunia.tokens.NiuniaSize.NavTop + com.nuvio.tv.skin.niunia.tokens.NiuniaSize.H44 + com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space4)
                .then(with(com.nuvio.tv.fork.glass.ForkGlass) { Modifier.forkGlass(com.nuvio.tv.fork.glass.ForkGlass.Role.Panel, shape, fallback = com.nuvio.tv.skin.niunia.tokens.NiuniaColor.Panel, fallbackBorder = com.nuvio.tv.skin.niunia.tokens.NiuniaColor.Fill12) })
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space16, vertical = com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space12)
                .niuniaTrapArrows("profile")
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space12),
        ) {
            val startIndex = profiles.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
            profiles.forEachIndexed { index, profile ->
                val me = profile.id == activeId
                com.nuvio.tv.skin.niunia.components.NiuniaProfileCard(
                    title = profile.name,
                    initials = com.nuvio.tv.skin.kicia.profile.forgeInitial(profile.name),
                    me = true,
                    current = me,
                    color = runCatching { Color(android.graphics.Color.parseColor(profile.avatarColorHex)) }
                        .onFailure { Log.w(TAG, "profile: kolor ${profile.avatarColorHex}: ${it.javaClass.simpleName}: ${it.message}") }.getOrNull(),
                    photo = avatarUrls[profile.id],
                    focusRequester = if (index == startIndex) first else null,
                    onFocus = { if (it) onProfileFocusChanged?.invoke(profile.avatarColorHex, profile.profileBackgroundUrl, null) },
                    onClick = { if (pinEnabled[profile.id] == true) { pinError = null; pinFor = profile } else select(profile) },
                )
            }
            if (canAdd) {
                com.nuvio.tv.skin.niunia.components.NiuniaProfileCard(
                    title = stringResource(R.string.niunia_profiles_add), icon = "plus",
                    focusRequester = if (profiles.isEmpty()) first else null,
                    onClick = { Log.i(TAG, "profile: dodaj profil"); onAddProfile() },
                )
            }
        }

        val target = pinFor
        if (target != null) {
            BackHandler { pinFor = null }
            NiuniaPinDialog(
                profileName = target.name,
                error = pinError,
                working = working,
                onCancel = { pinFor = null },
                onSubmit = { pin ->
                    viewModel.verifyProfilePin(target.id, pin) { result ->
                        result.onSuccess { verify ->
                            if (verify.unlocked) {
                                pinError = null; pinFor = null; select(target)
                            } else {
                                Log.i(TAG, "profile: zły PIN (blokada ${verify.retryAfterSeconds} s)")
                                pinError = if (verify.retryAfterSeconds > 0) context.getString(R.string.profile_pin_locked, verify.retryAfterSeconds)
                                else context.getString(R.string.profile_pin_invalid)
                            }
                        }.onFailure { e ->
                            Log.w(TAG, "profile: weryfikacja PIN: ${e.javaClass.simpleName}: ${e.message}")
                            pinError = context.getString(R.string.profile_pin_verify_error)
                        }
                    }
                },
            )
        }
    }
}

/** Jeden profil (canvas .prof 86 szer., .pav 80 dp, napis 12/16; fokus: awatar 1,08 + cień, napis biały). */
@Composable
private fun ProfileItem(
    name: String,
    focusRequester: FocusRequester?,
    current: Boolean,
    onFocus: () -> Unit,
    onOk: () -> Unit,
    avatar: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(86.dp), horizontalAlignment = Alignment.Start) { // pkt 3: od lewej
        Box {
            Box(
                Modifier
                    .size(80.dp)
                    .niuniaFocusable(CircleShape, scale = 1.08f, lift = false, focusRequester = focusRequester, onFocus = { focused = it; if (it) onFocus() }, onOk = onOk)
                    .clip(CircleShape)
                    .background(NiuniaColors.Fill24),
                contentAlignment = Alignment.Center,
            ) { avatar() }
            if (current) {
                // canvas .tick: 20 dp, kolor switch-on, biały znak.
                Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp).size(20.dp).background(Color(0xFF30D158), CircleShape), contentAlignment = Alignment.Center) {
                    Text("✓", style = NiuniaText.Caption.copy(fontSize = 12.sp, lineHeight = 12.sp), color = NiuniaColors.Text)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name, style = NiuniaText.Meta.copy(fontSize = 12.sp, lineHeight = 16.sp), color = if (focused) NiuniaColors.Text else NiuniaColors.Text70,
            // UX JMGO (ręczne testy 24.11): dwa profile „Stef+Patryk (K…” nie do odróżnienia — imię w dwóch liniach.
            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start,
        )
    }
}

/**
 * Okno PIN w stylu Niunii (Z-14, makieta ProfilePin): tytuł z nazwą profilu, cztery kropki postępu, klawiatura 3 × 4
 * (Anuluj, 0, Usuń), fokus startuje na środkowym klawiszu „5”; cyfry z pilota też działają; czwarta cyfra = sprawdzenie PIN-u.
 * Błędny PIN: kropki czyszczą się, komunikat pod tytułem.
 */
@Composable
private fun NiuniaPinDialog(profileName: String, error: String?, working: Boolean, onCancel: () -> Unit = {}, onSubmit: (String) -> Unit) {
    var digits by remember { mutableStateOf("") }
    val one = remember { FocusRequester() }
    fun type(d: String) {
        if (working || digits.length >= 4) return
        digits += d
        if (digits.length == 4) { Log.i(TAG, "PIN: 4 cyfry → sprawdzam"); onSubmit(digits) }
    }
    LaunchedEffect(Unit) {
        try { one.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "PIN: fokus: ${e.javaClass.simpleName}: ${e.message}") }
    }
    LaunchedEffect(error) { if (error != null) digits = "" }
    val cancel = stringResource(R.string.niunia_tv2_pin_cancel)
    val delete = stringResource(R.string.niunia_tv2_pin_delete)
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", cancel, "0", delete)
    // Pkt 3 (canvas v86): okno od lewej krawędzi treści (40 dp), zawartość od lewej.
    Box(Modifier.fillMaxSize().background(Color(0x99000000)).padding(start = 40.dp), contentAlignment = Alignment.CenterStart) {
        Column(
            with(com.nuvio.tv.fork.glass.ForkGlass) { Modifier.width(400.dp).forkGlass(com.nuvio.tv.fork.glass.ForkGlass.Role.Dialog, RoundedCornerShape(28.dp), fallback = Color(0xF53A3A3C), layer = LAYER_BACK) }.padding(24.dp).focusGroup()
                .onPreviewKeyEvent { e ->
                    // Cyfry z pilota (0–9) wpisują PIN bez chodzenia po klawiaturze.
                    if (e.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val code = e.nativeKeyEvent.keyCode
                    val d = when (code) {
                        in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9 -> code - android.view.KeyEvent.KEYCODE_0
                        in android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9 -> code - android.view.KeyEvent.KEYCODE_NUMPAD_0
                        else -> -1
                    }
                    if (d >= 0) { type(d.toString()); true } else false
                },
            horizontalAlignment = Alignment.Start,
        ) {
            Text(stringResource(R.string.niunia_profiles_pin, profileName), style = NiuniaText.Button.copy(fontSize = 20.sp, lineHeight = 26.sp), color = NiuniaColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (error != null) Text(error, style = NiuniaText.Body, color = Color(0xFFFF7B80), modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { i ->
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(NiuniaColors.Fill12), contentAlignment = Alignment.Center) {
                        if (i < digits.length) Box(Modifier.size(14.dp).background(NiuniaColors.Text, CircleShape))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { key ->
                            var focused by remember { mutableStateOf(false) }
                            Box(
                                Modifier
                                    .size(104.dp, 40.dp)
                                    .niuniaFocusable(RoundedCornerShape(50), scale = 1.06f, lift = false, focusRequester = if (key == "5") one else null, onFocus = { focused = it }, onOk = {
                                        if (!working) when (key) {
                                            delete -> digits = digits.dropLast(1)
                                            cancel -> { Log.i(TAG, "PIN: Anuluj"); onCancel() }
                                            else -> type(key)
                                        }
                                    })
                                    .background(if (focused) NiuniaColors.Focus else NiuniaColors.Fill12, RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center,
                            ) { Text(key, style = NiuniaText.Button, color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text) }
                        }
                    }
                }
            }
        }
    }
}

/** Ekran wyboru profilu otwarty z menu („Zmień profil”), nie na starcie aplikacji. */
object NiuniaProfileSwitch {
    @Volatile var fromMenu: Boolean = false
}
