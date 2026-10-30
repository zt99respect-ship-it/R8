package com.appletv.launcher.data

import android.graphics.Bitmap
import android.graphics.drawable.Drawable

data class AppModel(
    val label: String,
    val packageName: String,
    val activityName: String,
    val iconDrawable: Drawable? = null,
    val iconBitmap: Bitmap? = null,
    var isFavorite: Boolean = false,
    var launchCount: Int = 0,
    val dominantColor: Int = 0xFF1C1C1E.toInt(),
    val isLeanbackApp: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppModel) return false
        return packageName == other.packageName && isFavorite == other.isFavorite
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + isFavorite.hashCode()
        return result
    }
}
