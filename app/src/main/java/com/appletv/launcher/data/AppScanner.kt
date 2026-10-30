package com.appletv.launcher.data

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.palette.graphics.Palette

class AppScanner {

    /**
     * جلب جميع التطبيقات المثبتة في الشاشة مع تفضيل تطبيقات Leanback المخصصة للتلفزيون
     * واستخراج أيقوناتها الأصلية بدقة HD
     */
    fun scanInstalledApps(packageManager: PackageManager): List<AppModel> {
        val appList = mutableListOf<AppModel>()
        val seenPackages = HashSet<String>()

        // 1. فحص تطبيقات التلفزيون Leanback أولاً
        val tvIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val tvApps = packageManager.queryIntentActivities(tvIntent, 0)

        for (resolveInfo in tvApps) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == "com.appletv.launcher" || seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val model = createAppModel(resolveInfo, packageManager, isTv = true)
            appList.add(model)
        }

        // 2. فحص التطبيقات العادية (Sideloaded / Regular Apps) لضمان عدم ضياع أي تطبيق
        val standardIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val standardApps = packageManager.queryIntentActivities(standardIntent, 0)

        for (resolveInfo in standardApps) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == "com.appletv.launcher" || seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val model = createAppModel(resolveInfo, packageManager, isTv = false)
            appList.add(model)
        }

        // ترتيب التطبيقات: التطبيقات المفضلة أولاً، ثم أبجدياً
        return appList.sortedWith(compareByDescending<AppModel> { it.isFavorite }.thenBy { it.label })
    }

    private fun createAppModel(
        resolveInfo: ResolveInfo,
        pm: PackageManager,
        isTv: Boolean
    ): AppModel {
        val label = resolveInfo.loadLabel(pm).toString()
        val packageName = resolveInfo.activityInfo.packageName
        val activityName = resolveInfo.activityInfo.name

        // استخراج أفضل أيقونة أصلية متوفرة
        val iconDrawable = resolveInfo.loadIcon(pm)
        val iconBitmap = drawableToBitmap(iconDrawable)

        // استخراج اللون الأساسي لإنشاء توهج Ambient ناعم حول التطبيق
        val dominantColor = extractDominantColor(iconBitmap)

        return AppModel(
            label = label,
            packageName = packageName,
            activityName = activityName,
            iconDrawable = iconDrawable,
            iconBitmap = iconBitmap,
            dominantColor = dominantColor,
            isLeanbackApp = isTv
        )
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        return try {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 160
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 160
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun extractDominantColor(bitmap: Bitmap?): Int {
        if (bitmap == null) return 0xFF1C1C1E.toInt()
        return try {
            val palette = Palette.from(bitmap).generate()
            palette.getVibrantColor(palette.getDominantColor(0xFF2C2C2E.toInt()))
        } catch (e: Exception) {
            0xFF2C2C2E.toInt()
        }
    }
}
