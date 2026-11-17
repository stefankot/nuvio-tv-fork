package com.nuvio.tv.fork.launcher

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Opcje systemowe (B06): start z telewizorem i Nuvio jako ekran główny. Aplikacja przełącza tylko to, co Android pozwala jej
 * przełączyć sama; resztę otwiera jako ekran systemu. (Kod z Kicia, bez asystenta.)
 */
object LauncherOptions {
    private const val PREFS = "fork_launcher"
    const val HOME_ALIAS = "com.nuvio.tv.launcher.ForkHomeAlias"

    fun autostart(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("autostart", false)
    fun setAutostart(context: Context, on: Boolean) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("autostart", on).apply()

    fun canStartFromBackground(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(context)
    fun openOverlayPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    fun homeEnabled(context: Context): Boolean = runCatching {
        context.packageManager.getComponentEnabledSetting(ComponentName(context.packageName, HOME_ALIAS)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }.getOrDefault(false)

    fun isDefaultHome(context: Context): Boolean = runCatching {
        val home = context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
        home?.activityInfo?.packageName == context.packageName
    }.getOrDefault(false)

    /** Włącza (lub nie) alias HOME i otwiera systemowy wybór ekranu głównego. Alias tylko, gdy trasa „Aplikacje” istnieje (B06-2). */
    fun setHome(context: Context, on: Boolean) {
        runCatching {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context.packageName, HOME_ALIAS),
                if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        if (on) {
            runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure {
                runCatching { context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
    }

    fun openSystemSettings(context: Context) {
        runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openInputs(context: Context): Boolean = runCatching {
        context.startActivity(Intent("android.settings.INPUT_METHOD_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    }.getOrDefault(false)
}

/** Uruchamia Nuvio po starcie telewizora, gdy włączono „Uruchamiaj z telewizorem”. */
class ForkBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != "android.intent.action.QUICKBOOT_POWERON") return
        if (!LauncherOptions.autostart(context)) return
        val launch = context.packageManager.getLeanbackLaunchIntentForPackage(context.packageName)
            ?: context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        runCatching { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
