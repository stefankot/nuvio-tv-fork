package com.nuvio.tv.skin.kicia

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Settings → Nuvio Forge: start with the TV, Nuvio as the home screen, Nuvio as the assistant (owner 8.11).
 * Each only switches what Android lets an app switch itself; the rest opens the system screen that does it.
 */
object ForgeSystemOptions {
    private const val PREFS = "forge_system"
    private const val HOME_ALIAS = "com.nuvio.tv.launcher.ForgeHomeAlias"

    // ---- start with the TV ----
    fun autostart(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("autostart", false)
    fun setAutostart(context: Context, on: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("autostart", on).apply()

    /** Android lets an app open itself after boot only with "display over other apps" on most TVs. */
    fun canStartFromBackground(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(context)
    fun openOverlayPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    // ---- home screen ----
    fun homeEnabled(context: Context): Boolean = runCatching {
        context.packageManager.getComponentEnabledSetting(ComponentName(context.packageName, HOME_ALIAS)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }.getOrDefault(false)

    fun isDefaultHome(context: Context): Boolean = runCatching {
        val home = context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
        home?.activityInfo?.packageName == context.packageName
    }.getOrDefault(false)

    /** Makes Nuvio offerable as the home screen (or not) and opens the system choice of home app. */
    fun setHome(context: Context, on: Boolean) {
        runCatching {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context.packageName, HOME_ALIAS),
                if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        if (on) {
            val settings = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(settings) }.onFailure {
                // No home settings screen (most TVs): pressing Home then asks which home app to use.
                runCatching { context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
    }

    // ---- assistant ----
    fun isAssistant(context: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
        } else {
            Settings.Secure.getString(context.contentResolver, "assistant")?.contains(context.packageName) == true
        }
    }.getOrDefault(false)

    /** The system screen where the assistant is chosen (the role cannot be asked for by an app). */
    fun openAssistantSettings(context: Context) {
        val candidates = listOf(
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in candidates) {
            if (runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }
}

/** Starts Nuvio after the TV boots when "Uruchamiaj z telewizorem" is on. */
class ForgeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != "android.intent.action.QUICKBOOT_POWERON") return
        if (!ForgeSystemOptions.autostart(context)) return
        val launch = context.packageManager.getLeanbackLaunchIntentForPackage(context.packageName)
            ?: context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        runCatching { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
