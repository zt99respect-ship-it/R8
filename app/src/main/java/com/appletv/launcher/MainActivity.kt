package com.appletv.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appletv.launcher.adapter.AppsAdapter
import com.appletv.launcher.data.AppModel
import com.appletv.launcher.data.AppScanner
import com.appletv.launcher.util.TvFocusHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var appsAdapter: AppsAdapter
    private lateinit var tvClock: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvFocusedTitle: TextView
    private lateinit var tvFocusedPackage: TextView
    private lateinit var ivTopShelfBackdrop: ImageView
    private lateinit var ambientGlowView: View

    private val appScanner = AppScanner()
    private val clockHandler = Handler(Looper.getMainLooper())
    private var lastBackPressTime: Long = 0

    // تحديث التطبيقات تلقائياً عند تثبيت تطبيق جديد
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadInstalledApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // إخفاء شريط النظام لشاشة سينمائية كاملة 16:9
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupClock()
        setupBackPressInterceptor()
        registerPackageReceiver()

        loadInstalledApps()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rv_apps)
        tvClock = findViewById(R.id.tv_clock)
        tvDate = findViewById(R.id.tv_date)
        tvFocusedTitle = findViewById(R.id.tv_focused_title)
        tvFocusedPackage = findViewById(R.id.tv_focused_package)
        ivTopShelfBackdrop = findViewById(R.id.iv_top_shelf_backdrop)
        ambientGlowView = findViewById(R.id.view_ambient_glow)
    }

    private fun setupRecyclerView() {
        // شبكة أفقية مرنة بـ 5 أعمدة لتطابق شبكة Apple TV 4K
        val layoutManager = GridLayoutManager(this, 5)
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(30) // تحسين أقصى أداء 60 FPS

        appsAdapter = AppsAdapter(
            onAppClick = { app ->
                launchApp(app)
            },
            onAppFocused = { app, view ->
                onAppFocusChanged(app, view)
            },
            onAppLongClick = { app ->
                toggleAppFavorite(app)
            }
        )
        recyclerView.adapter = appsAdapter
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch(Dispatchers.IO) {
            // جلب أيقونات التطبيقات الأصلية عالية الدقة بدون إيموجي أو أيقونات رديئة
            val apps = appScanner.scanInstalledApps(packageManager)
            withContext(Dispatchers.Main) {
                appsAdapter.submitList(apps)
                if (apps.isNotEmpty()) {
                    onAppFocusChanged(apps[0], null)
                    recyclerView.post {
                        recyclerView.getChildAt(0)?.requestFocus()
                    }
                }
            }
        }
    }

    private fun onAppFocusChanged(app: AppModel, view: View?) {
        tvFocusedTitle.text = app.label
        tvFocusedPackage.text = app.packageName
        
        // تطبيق تأثير توهج خلفي ديناميكي (Ambient Backlight) بلون التطبيق
        TvFocusHelper.applyAmbientColor(ambientGlowView, app.dominantColor)
    }

    private fun launchApp(app: AppModel) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } else {
                Toast.makeText(this, "تعذر تشغيل التطبيق", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleAppFavorite(app: AppModel) {
        app.isFavorite = !app.isFavorite
        appsAdapter.notifyDataSetChanged()
        val msg = if (app.isFavorite) "تمت إضافة التطبيق للمفضلة في الصف الأول" else "تمت إزالة التطبيق من المفضلة"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * منع الخروج العرضي من اللانشر عند الضغط على زر الرجوع (Back) بالريموت
     * ليبقى هو الواجهة الافتراضية الثابتة دائماً
     */
    private fun setupBackPressInterceptor() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentTime = System.currentTimeMillis()
                // إذا لم يكن التركيز على أول عنصر، أعد التركيز إلى أول تطبيق
                val firstView = recyclerView.getChildAt(0)
                if (firstView != null && !firstView.hasFocus()) {
                    firstView.requestFocus()
                    recyclerView.smoothScrollToPosition(0)
                    return
                }

                // منع الخروج نهائياً من اللانشر وإظهار إشعار لطيف
                if (currentTime - lastBackPressTime > 2500) {
                    Toast.makeText(
                        this@MainActivity,
                        "أنت في الشاشة الرئيسية (Apple TV Launcher)",
                        Toast.LENGTH_SHORT
                    ).show()
                    lastBackPressTime = currentTime
                }
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // التأكد من استجابة أزرار ريموت التلفزيون بسلاسة
        when (keyCode) {
            KeyEvent.KEYCODE_HOME -> {
                recyclerView.smoothScrollToPosition(0)
                recyclerView.getChildAt(0)?.requestFocus()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun setupClock() {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE، d MMMM", Locale("ar"))

        val clockRunnable = object : Runnable {
            override fun run() {
                val now = Calendar.getInstance().time
                tvClock.text = timeFormat.format(now)
                tvDate.text = dateFormat.format(now)
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable)
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(packageReceiver)
        } catch (ignored: Exception) {}
    }
}
