package com.viennnaa.utilities.core.shortcuts

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.viennnaa.utilities.MainActivity
import com.viennnaa.utilities.miniapp.MiniApp

/**
 * Long-press shortcuts on the launcher icon, built from the mini app catalog.
 *
 * Each shortcut opens its mini app through the same deep link the navigation
 * graph already handles, so there is one way in rather than a second entry point
 * to keep in step.
 */

private const val ICON_SIZE = 192

/** Where the emoji sits inside the icon, leaving room for the adaptive mask. */
private const val EMOJI_SCALE = 0.42f

/**
 * Replaces the dynamic shortcuts with the first few mini apps in catalog order.
 *
 * The launcher decides how many it will show, so asking for more than it
 * reports would just be silently trimmed.
 */
fun publishMiniAppShortcuts(context: Context, miniApps: List<MiniApp>) {
    val limit = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        .coerceAtMost(miniApps.size)
    if (limit <= 0) return

    val shortcuts = miniApps.take(limit).map { miniApp ->
        val label = context.getString(miniApp.titleRes)
        ShortcutInfoCompat.Builder(context, miniApp.id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(emojiIcon(miniApp.emoji, miniApp.accent.toArgb()))
            .setIntent(
                Intent(Intent.ACTION_VIEW, miniAppDeepLink(miniApp.id), context, MainActivity::class.java),
            )
            .build()
    }
    // Failure here is cosmetic - a launcher that refuses shortcuts should not
    // take the app down on startup.
    runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
}

/** The deep link that opens [miniAppId], also handled by the navigation graph. */
fun miniAppDeepLink(miniAppId: String): Uri = Uri.parse("$DEEP_LINK_PREFIX$miniAppId")

/** Scheme and host are matched by the intent filter in the manifest. */
const val DEEP_LINK_PREFIX = "utilities://miniapp/"

/**
 * Draws the mini app's emoji on its accent colour, so a shortcut is recognisable
 * as the same thing as its home screen tile.
 */
private fun emojiIcon(emoji: String, accent: Int): IconCompat {
    val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(accent)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = ICON_SIZE * EMOJI_SCALE
        textAlign = Paint.Align.CENTER
    }
    // Centre on the text's own box rather than its origin, which sits on the
    // baseline and would push the glyph high.
    val baseline = ICON_SIZE / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(emoji, ICON_SIZE / 2f, baseline, textPaint)
    return IconCompat.createWithAdaptiveBitmap(bitmap)
}
