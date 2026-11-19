package com.nuvio.tv.skin.kicia

import android.app.Activity
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.fork.logged

/**
 * forge (owner 13.11.2026): a small, quiet build label floating in the bottom-right corner of every screen, so a
 * screenshot or a look at the TV tells which build is running. It sits on the window's decor view, above the Compose
 * content, takes no focus and no touches. The commit and build time come from `assets/forge_build.txt`, written by
 * the build automation (`forge-build.sh`); without it only the version is shown.
 */
object ForgeBuildBadge {
    private const val TAG = "forge_build_badge"

    fun attach(activity: Activity) {
        val decor = activity.window?.decorView as? FrameLayout ?: return
        if (decor.findViewWithTag<View>(TAG) != null) return
        val build = runCatching {
            activity.assets.open("forge_build.txt").bufferedReader().use { it.readText().trim() }
        }.logged("ForkKiciaCore", "ForgeBuildBadge.kt:24").getOrNull()?.takeIf { it.isNotEmpty() }
        val label = TextView(activity).apply {
            tag = TAG
            text = listOfNotNull("v${BuildConfig.VERSION_NAME}", build).joinToString(" · ")
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setTextColor(Color.argb(110, 255, 255, 255))
            setShadowLayer(2f, 0f, 1f, Color.argb(140, 0, 0, 0))
            isFocusable = false
            isClickable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, activity.resources.displayMetrics).toInt()
        decor.addView(
            label,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply { setMargins(margin, margin, margin, margin) }
        )
    }
}
