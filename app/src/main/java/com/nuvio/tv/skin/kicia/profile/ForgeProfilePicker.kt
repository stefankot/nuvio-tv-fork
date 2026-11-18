package com.nuvio.tv.skin.kicia.profile

import com.nuvio.tv.skin.kicia.nav.forgeTagRoot
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.testTag
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.UserProfile
import com.nuvio.tv.ui.components.ProfileAvatarCircle
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.screens.profile.ProfileSelectionViewModel

/**
 * "Kto ogląda?" (S15): a row of round avatars, focus = scale + white ring; a profile with a PIN opens
 * the four-wheel PIN dialog. Verification and selecting the profile are Nuvio's own (view model); only
 * the look and the key handling are new. Creating / editing profiles stays on Nuvio's management screen.
 */
@Composable
fun ForgeProfilePicker(
    viewModel: ProfileSelectionViewModel,
    onAddProfile: (() -> Unit)? = null,
    onProfileSelected: () -> Unit,
    onProfileClicked: () -> Unit,
    onProfileSelectionFailed: () -> Unit,
    onProfileFocusChanged: ((colorHex: String?, backgroundUrl: String?, memoryCacheKey: String?) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val pinEnabled by viewModel.profilePinEnabled.collectAsState()
    val working by viewModel.isPinOperationInProgress.collectAsState()
    val avatarCatalog by viewModel.avatarCatalog.collectAsState()
    // Same mapping as Nuvio's own picker: the catalog when it has arrived, else the repository's lookup.
    val avatarUrls = remember(avatarCatalog, profiles) { forgeAvatarUrls(profiles, avatarCatalog.associate { it.id to it.imageUrl }, viewModel::getAvatarImageUrl) }
    // Owner 10.11: the avatars of the Nuvio account did not show. The catalog was asked for once, at start, often before the account was signed
    // in and synced, and never again, so a profile with an avatar id had no picture. While any profile's picture is missing, the catalog is asked
    // for again (4 tries: 2, 4, 8, 16 s apart); a profile's own address needs no catalog.
    val avatarMissing = profiles.any { p -> p.avatarUrl.isNullOrBlank() && !p.avatarId.isNullOrBlank() && avatarUrls[p.id].isNullOrBlank() }
    LaunchedEffect(avatarMissing, profiles.size) {
        var wait = 2_000L
        repeat(4) {
            if (!avatarMissing) return@LaunchedEffect
            kotlinx.coroutines.delay(wait)
            viewModel.loadAvatarCatalog()
            wait *= 2
        }
    }
    var pinFor by remember { mutableStateOf<UserProfile?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }
    val first = remember { FocusRequester() }

    fun select(profile: UserProfile) {
        onProfileClicked()
        viewModel.selectProfile(profile.id, onComplete = onProfileSelected, onFailure = onProfileSelectionFailed)
    }

    LaunchedEffect(profiles.isNotEmpty()) {
        if (profiles.isNotEmpty()) runCatching { first.requestFocus() }
    }

    Box(modifier.fillMaxSize().background(ForgeColors.Bg).forgeTagRoot()) { // v7.4 (02): tagi widoczne w uiautomator (ekran przed paskiem)
        // v7.4 (02): flat background, no light behind the profiles.
        Column(
            Modifier.fillMaxSize().padding(top = 150.dp), // v7.4: title y 150
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Kto ogląda?", style = ForgeText.title, textAlign = TextAlign.Center, modifier = Modifier.height(38.dp).testTag("kto-tytul.0")) // v7.4 (02): 32/38
            Spacer(Modifier.height(12.dp)) // v7.4: avatars from y 212 (title 150 + 38 + 12 + row padding 12)
            // Centred when the profiles fit; scrolls with the focus when they don't (no avatar cut at the edge).
            Row(
                Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(start = ((960 - ((profiles.size + if (onAddProfile != null && viewModel.canAddProfile) 1 else 0) * 180 - 20)) / 2).coerceAtLeast(40).dp, end = 40.dp).padding(vertical = 12.dp), // v7.4: centred, column 160 + 20
                horizontalArrangement = Arrangement.spacedBy(20.dp), // v7.4: columns 160, gap 20
                verticalAlignment = Alignment.Top,
            ) {
                profiles.forEachIndexed { index, profile ->
                    Column(Modifier.width(160.dp), horizontalAlignment = Alignment.CenterHorizontally) { // v7.4: column 160
                        ForgeFocusBox(
                            radius = 60.dp,
                            modifier = Modifier.size(120.dp).testTag("awatar.$index"),
                            outline = true,
                            focusRequester = if (index == 0) first else null,
                            onFocusChange = { focused ->
                                if (focused) onProfileFocusChanged?.invoke(profile.avatarColorHex, profile.profileBackgroundUrl, null)
                            },
                            onOk = {
                                if (pinEnabled[profile.id] == true) {
                                    pinError = null
                                    pinFor = profile
                                } else {
                                    select(profile)
                                }
                            },
                        ) { _ ->
                            Box(Modifier.size(120.dp).clip(CircleShape)) {
                                // Initial underneath, picture on top: a picture that fails to load leaves the initial, not an empty circle.
                                ForgeInitialAvatar(name = profile.name, colorHex = profile.avatarColorHex)
                                avatarUrls[profile.id]?.let { url ->
                                    coil3.compose.AsyncImage(
                                        model = url,
                                        contentDescription = profile.name,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.size(120.dp).clip(CircleShape),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text( // v7.4 (02): name 16/700, up to 2 lines, centred, 160 wide
                            profile.name, style = ForgeText.shelfTitle, maxLines = 2, textAlign = TextAlign.Center,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.width(160.dp).height(44.dp).testTag("profil-nazwa.$index"),
                        )
                    }
                }
                // "Dodaj profil" (mockup S15): opens Nuvio's own create-profile dialog.
                if (onAddProfile != null && viewModel.canAddProfile) {
                    Column(Modifier.width(160.dp), horizontalAlignment = Alignment.CenterHorizontally) { // v7.4: column 160
                        ForgeFocusBox(
                            radius = 60.dp,
                            modifier = Modifier.size(120.dp),
                            outline = true,
                            focusRequester = if (profiles.isEmpty()) first else null,
                            onOk = onAddProfile,
                        ) { _ ->
                            Box(
                                Modifier.size(120.dp).clip(CircleShape).background(ForgeColors.Surface),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("+", style = ForgeText.title)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Dodaj profil", style = ForgeText.label, maxLines = 1)
                    }
                }
            }
        }

        val target = pinFor
        if (target != null) {
            BackHandler { pinFor = null }
            ForgePinDialog(
                profileName = target.name,
                error = pinError,
                working = working,
                onSubmit = { pin ->
                    viewModel.verifyProfilePin(target.id, pin) { result ->
                        result.onSuccess { verify ->
                            if (verify.unlocked) {
                                pinError = null
                                pinFor = null
                                select(target)
                            } else {
                                pinError = if (verify.retryAfterSeconds > 0) {
                                    context.getString(R.string.profile_pin_locked, verify.retryAfterSeconds)
                                } else {
                                    context.getString(R.string.profile_pin_invalid)
                                }
                            }
                        }.onFailure {
                            pinError = context.getString(R.string.profile_pin_verify_error)
                        }
                    }
                },
            )
        }
    }
}

/**
 * v7.4 (02): Okno PIN 440: "Podaj PIN profilu <nazwa>" 20/700, four fields 44 (r 8, 12% fill) with a 16 dp dot per digit,
 * under them the digit keys 116 x 36 (gap 4) 1-9, ⌫, 0, OK; the focus starts on "1". A wrong PIN: Komunikat, the fields clear.
 */
@Composable
private fun ForgePinDialog(
    profileName: String,
    error: String?,
    working: Boolean,
    onSubmit: (String) -> Unit,
) {
    var digits by remember { androidx.compose.runtime.mutableStateOf("") }
    val one = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { one.requestFocus() } }
    LaunchedEffect(error) { if (error != null) { digits = ""; com.nuvio.tv.skin.kicia.components.ForgeToast.show(error) } }
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK")

    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x99000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(ForgeDims.RadiusSheet))
                .background(ForgeColors.GlassFallback)
                .testTag("okno.0")
                .padding(24.dp)
                .focusGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Podaj PIN profilu $profileName", style = ForgeText.head, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().testTag("okno-tytul.0"))
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { i ->
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(ForgeColors.Surface).testTag("pin-pole.$i"), contentAlignment = Alignment.Center) {
                        if (i < digits.length) Box(Modifier.size(16.dp).background(ForgeColors.Text, CircleShape))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                keys.chunked(3).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEachIndexed { c, key ->
                            val n = r * 3 + c
                            ForgeFocusBox(
                                radius = 8.dp,
                                modifier = Modifier.size(116.dp, 36.dp).testTag("klawisz.$n"),
                                focusRequester = if (key == "1") one else null,
                                onLongOk = if (key == "⌫") ({ digits = "" }) else null,
                                onOk = {
                                    if (!working) when (key) {
                                        "⌫" -> digits = digits.dropLast(1)
                                        "OK" -> if (digits.length == 4) onSubmit(digits)
                                        else -> if (digits.length < 4) digits += key
                                    }
                                },
                            ) { focused ->
                                Box(
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(if (focused) ForgeColors.Focus else ForgeColors.Surface),
                                    contentAlignment = Alignment.Center,
                                ) { Text(key, style = ForgeText.body.copy(color = if (focused) ForgeColors.OnFocus else ForgeColors.Text)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Avatar picture per profile id: the profile's own picture when it has one (a blank address counts as none,
 * so the circle shows the initial instead of an empty picture), else the catalog avatar.
 */
internal fun forgeAvatarUrls(
    profiles: List<UserProfile>,
    catalog: Map<String, String?>,
    lookup: (String?) -> String?,
): Map<Int, String?> = profiles.associate { p ->
    p.id to (p.avatarUrl?.takeIf { it.isNotBlank() }
        ?: p.avatarId?.let { id -> catalog[id] ?: lookup(id) }?.takeIf { it.isNotBlank() })
}

/** The profile's initial on its colour; the letter turns dark on a light colour so it stays readable. */
@Composable
private fun ForgeInitialAvatar(name: String, colorHex: String) {
    val bg = ForgeColors.Surface2 // v7.4: neutral avatar #3A3A3C for every profile (the profile colour is no longer shown)
    Box(Modifier.size(120.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
        Text(
            text = forgeInitial(name),
            style = ForgeText.display.copy(color = if (forgeIsLight(bg.red, bg.green, bg.blue)) ForgeColors.OnFocus else ForgeColors.Text),
        )
    }
}

/** First letter or digit of the name, upper case ("?" when there is none). */
internal fun forgeInitial(name: String): String = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

/** True for colours light enough that white text on them is hard to read (relative luminance > 0.6). */
internal fun forgeIsLight(r: Float, g: Float, b: Float): Boolean {
    fun lin(c: Float) = if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * lin(r) + 0.7152f * lin(g) + 0.0722f * lin(b) > 0.6f
}
