package com.lvngx.gamespace

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FloatingPanelService : Service() {

    private lateinit var wm: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null
    private var crosshairView: View? = null
    private var clickerBubbleView: View? = null
    private var fpsBadgeView: TextView? = null

    private var isPanelOpen = false
    private var currentFps = 60
    private var fpsTracker: RealTimeFpsTracker? = null

    // Auto Clicker State (Zayon style)
    private var isAutoClickerRunning = false
    private val clickHandler = Handler(Looper.getMainLooper())
    private lateinit var clickRunnable: Runnable

    private val colNavyDark = Color.parseColor("#EE060E20")
    private val colNavyCard = Color.parseColor("#FF0D1B38")
    private val colCrimson = Color.parseColor("#FFE91E5A")
    private val colCrimsonSoft = Color.parseColor("#33E91E5A")
    private val colCyan = Color.parseColor("#FF38BDF8")

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        createFloatingBubble()
        createAutoClickerBubble()

        fpsTracker = RealTimeFpsTracker { fps ->
            currentFps = fps
            fpsBadgeView?.text = "FPS $fps"
        }.also { it.start() }

        if (GameSpaceStore.isCrosshairEnabled(this)) {
            showCrosshairOverlay(true)
        }

        setupClickRunnable()
    }

    private fun setupClickRunnable() {
        clickRunnable = object : Runnable {
            override fun run() {
                if (!isAutoClickerRunning) return
                if (!ShizukuHelper.isShizukuRunning() || !ShizukuHelper.hasShizukuPermission()) {
                    isAutoClickerRunning = false
                    Toast.makeText(this@FloatingPanelService, "Auto-Clicker berhenti: Shizuku tidak aktif/izin mati!", Toast.LENGTH_SHORT).show()
                    return
                }
                val (x, y) = GameSpaceStore.getClickCoord(this@FloatingPanelService)
                ShizukuHelper.tap(x, y)

                val cps = GameSpaceStore.getClickCps(this@FloatingPanelService).coerceIn(1, 50)
                val interval = (1000L / cps).coerceAtLeast(20L)
                clickHandler.postDelayed(this, interval)
            }
        }
    }

    private fun startAutoClicker() {
        if (!ShizukuHelper.isShizukuRunning() || !ShizukuHelper.hasShizukuPermission()) {
            Toast.makeText(this, "Shizuku wajib aktif & diberi izin untuk Auto-Clicker!", Toast.LENGTH_LONG).show()
            return
        }
        isAutoClickerRunning = true
        clickHandler.post(clickRunnable)
        Toast.makeText(this, "⚡ Auto-Clicker Dimulai (${GameSpaceStore.getClickCps(this)} CPS)", Toast.LENGTH_SHORT).show()
    }

    private fun stopAutoClicker() {
        isAutoClickerRunning = false
        clickHandler.removeCallbacks(clickRunnable)
        Toast.makeText(this, "Auto-Clicker Dihentikan", Toast.LENGTH_SHORT).show()
    }

    private fun startForegroundNotification() {
        val channelId = "lvngx_zayon_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "LVNG-X Zayon Game Space", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("LVNG-X Game Space Active")
                .setContentText("Zayon Floating Panel & Shizuku Auto-Clicker siap")
                .setSmallIcon(R.drawable.ic_lvngx_logo)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("LVNG-X Game Space")
                .setContentText("Active")
                .setSmallIcon(R.drawable.ic_lvngx_logo)
                .build()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(101, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(101, notif)
        }
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun roundedBg(fillColor: Int, strokeColor: Int = Color.TRANSPARENT, radiusDp: Float = 16f, strokeWidthPx: Int = 2): GradientDrawable {
        val density = resources.displayMetrics.density
        return GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = radiusDp * density
            if (strokeColor != Color.TRANSPARENT) setStroke(strokeWidthPx, strokeColor)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingBubble() {
        val container = FrameLayout(this).apply {
            background = roundedBg(colNavyDark, colCrimson, 18f, 3)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_lvngx_logo)
            layoutParams = FrameLayout.LayoutParams(dp(38), dp(38))
        }
        container.addView(logo)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(160)
        }

        var initX = 0; var initY = 0; var initTouchX = 0f; var initTouchY = 0f; var moved = false
        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y; initTouchX = event.rawX; initTouchY = event.rawY; moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initTouchX).toInt()
                    val dy = (event.rawY - initTouchY).toInt()
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                    params.x = initX + dx; params.y = initY + dy
                    wm.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) toggleMainPanel()
                    true
                }
                else -> false
            }
        }

        bubbleView = container
        wm.addView(container, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createAutoClickerBubble() {
        val circle = FrameLayout(this).apply {
            background = roundedBg(colCrimson, colCyan, 24f, 2)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        val label = TextView(this).apply {
            text = "⚡"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(dp(32), dp(32))
        }
        circle.addView(label)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(20)
            y = dp(300)
        }

        var initX = 0; var initY = 0; var initTouchX = 0f; var initTouchY = 0f; var moved = false
        circle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y; initTouchX = event.rawX; initTouchY = event.rawY; moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initTouchX).toInt()
                    val dy = (event.rawY - initTouchY).toInt()
                    if (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6) moved = true
                    params.x = initX + dx; params.y = initY + dy
                    wm.updateViewLayout(circle, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        // Toggle Auto-Clicker on tap
                        if (isAutoClickerRunning) {
                            stopAutoClicker()
                            circle.background = roundedBg(colCrimson, colCyan, 24f, 2)
                        } else {
                            startAutoClicker()
                            circle.background = roundedBg(Color.parseColor("#FF10B981"), Color.WHITE, 24f, 3)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        clickerBubbleView = circle
        wm.addView(circle, params)
    }

    private fun toggleMainPanel() {
        if (isPanelOpen) closeMainPanel() else openMainPanel()
    }

    private fun closeMainPanel() {
        panelView?.let { runCatching { wm.removeView(it) } }
        panelView = null
        isPanelOpen = false
    }

    @SuppressLint("SetTextI18n")
    private fun openMainPanel() {
        closeMainPanel()
        isPanelOpen = true

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(colNavyDark, colCrimson, 20f, 3)
            setPadding(dp(16), dp(14), dp(16), dp(16))
        }

        // Header: Logo + Title + FPS + Close
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val miniLogo = ImageView(this).apply {
            setImageResource(R.drawable.ic_lvngx_logo)
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
        }
        val title = TextView(this).apply {
            text = "  ZAYON • LVNG-X"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val fpsText = TextView(this).apply {
            text = "FPS $currentFps"
            setTextColor(colCyan)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(colNavyCard, colCyan, 8f, 1)
            setPadding(dp(8), dp(3), dp(8), dp(3))
        }
        fpsBadgeView = fpsText
        val closeBtn = TextView(this).apply {
            text = "  ✕ "
            setTextColor(colCrimson)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener { closeMainPanel() }
        }

        header.addView(miniLogo); header.addView(title); header.addView(fpsText); header.addView(closeBtn)
        root.addView(header)

        val shizukuReady = ShizukuHelper.isShizukuRunning() && ShizukuHelper.hasShizukuPermission()
        val (usedRam, totalRam) = PerformanceBooster.getRamStats(this)

        val statusInfo = TextView(this).apply {
            text = "Shizuku: ${if (shizukuReady) "🟢 AKTIF" else "🔴 MATI"} • RAM: ${usedRam}MB / ${totalRam}MB"
            setTextColor(if (shizukuReady) colCyan else Color.parseColor("#FFF87171"))
            textSize = 11f
            setPadding(0, dp(6), 0, dp(10))
        }
        root.addView(statusInfo)

        // Row 1: Action Tiles (Boost | Audio | Crosshair)
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        
        val btnBoost = makeTile("⚡ BOOST", "Clear RAM", true) {
            val res = PerformanceBooster.boostMemory(this)
            Toast.makeText(this, "Boosted +${res.freedMb} MB!", Toast.LENGTH_SHORT).show()
        }
        var audioOn = GameSpaceStore.isAudioBoost(this)
        val btnAudio = makeTile("🔊 AUDIO", if (audioOn) "FX ON" else "FX OFF", audioOn) {
            audioOn = !audioOn
            GameSpaceStore.setAudioBoost(this, audioOn)
            PerformanceBooster.toggleAudioFx(this, audioOn)
            openMainPanel()
        }
        var crossOn = GameSpaceStore.isCrosshairEnabled(this)
        val btnCross = makeTile("⊕ AIM", if (crossOn) "ON" else "OFF", crossOn) {
            crossOn = !crossOn
            GameSpaceStore.setCrosshairEnabled(this, crossOn)
            showCrosshairOverlay(crossOn)
            openMainPanel()
        }

        row1.addView(btnBoost); row1.addView(space(8)); row1.addView(btnAudio); row1.addView(space(8)); row1.addView(btnCross)
        root.addView(row1)
        root.addView(space(10))

        // Zayon Auto-Clicker Card (Shizuku powered)
        val clickerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(colNavyCard, Color.parseColor("#FF1E3A6E"), 14f, 2)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val curCps = GameSpaceStore.getClickCps(this)
        val clickerHeader = TextView(this).apply {
            text = "⚡ AUTO-CLICKER (SHIZUKU) • $curCps CPS"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        clickerCard.addView(clickerHeader)
        clickerCard.addView(space(6))

        val cpsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val btnMinus = makeButton("-") {
            val c = (GameSpaceStore.getClickCps(this) - 2).coerceIn(1, 50)
            GameSpaceStore.setClickCps(this, c)
            openMainPanel()
        }
        val cpsLabel = TextView(this).apply {
            text = "$curCps Klik/detik"
            setTextColor(colCyan)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnPlus = makeButton("+") {
            val c = (GameSpaceStore.getClickCps(this) + 2).coerceIn(1, 50)
            GameSpaceStore.setClickCps(this, c)
            openMainPanel()
        }

        cpsRow.addView(btnMinus); cpsRow.addView(cpsLabel); cpsRow.addView(btnPlus)
        clickerCard.addView(cpsRow)
        clickerCard.addView(space(8))

        val clickerActionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnToggleClicker = TextView(this).apply {
            text = if (isAutoClickerRunning) "■ STOP AUTO-CLICKER" else "▶ MULAI AUTO-CLICKER"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = roundedBg(if (isAutoClickerRunning) Color.parseColor("#FFDC2626") else colCrimson, Color.TRANSPARENT, 10f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f)
            setOnClickListener {
                if (isAutoClickerRunning) stopAutoClicker() else startAutoClicker()
                openMainPanel()
            }
        }
        val btnSetCoord = TextView(this).apply {
            text = "🎯 POSISI TAP"
            setTextColor(colCyan)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = roundedBg(Color.parseColor("#FF0B172E"), colCyan, 10f, 1)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                closeMainPanel()
                Toast.makeText(this@FloatingPanelService, "Tombol bulat oranye di layar bisa digeser untuk posisi klik!", Toast.LENGTH_LONG).show()
            }
        }

        clickerActionRow.addView(btnToggleClicker)
        clickerActionRow.addView(space(8))
        clickerActionRow.addView(btnSetCoord)
        clickerCard.addView(clickerActionRow)

        root.addView(clickerCard)
        root.addView(space(10))

        // Footer: Full App & Stop
        val footer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnApp = TextView(this).apply {
            text = "⚙ MENU UTAMA"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            background = roundedBg(colNavyCard, Color.TRANSPARENT, 8f)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                closeMainPanel()
                startActivity(Intent(this@FloatingPanelService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        val btnCloseSvc = TextView(this).apply {
            text = "TUTUP PANEL"
            setTextColor(Color.parseColor("#FFF87171"))
            textSize = 11f
            gravity = Gravity.CENTER
            background = roundedBg(colCrimsonSoft, colCrimson, 8f, 1)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { stopSelf() }
        }
        footer.addView(btnApp); footer.addView(space(8)); footer.addView(btnCloseSvc)
        root.addView(footer)

        val params = WindowManager.LayoutParams(
            dp(300), WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        panelView = root
        wm.addView(root, params)
    }

    private fun makeTile(title: String, sub: String, active: Boolean, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = roundedBg(if (active) colCrimsonSoft else colNavyCard, if (active) colCrimson else Color.parseColor("#FF1E3A6E"), 12f, 2)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { onClick() }

            addView(TextView(this@FloatingPanelService).apply {
                text = title; setTextColor(Color.WHITE); textSize = 11f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            })
            addView(TextView(this@FloatingPanelService).apply {
                text = sub; setTextColor(if (active) colCrimson else Color.parseColor("#FF94A3B8")); textSize = 10f; gravity = Gravity.CENTER
            })
        }
    }

    private fun makeButton(text: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = roundedBg(colNavyCard, colCyan, 8f, 1)
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setOnClickListener { onClick() }
        }
    }

    private fun space(sizePx: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(sizePx), dp(sizePx)) }

    private fun showCrosshairOverlay(show: Boolean) {
        crosshairView?.let { runCatching { wm.removeView(it) } }
        crosshairView = null
        if (!show) return

        val view = object : View(this) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colCrimson; style = Paint.Style.STROKE; strokeWidth = 4f }
            private val center = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colCyan; style = Paint.Style.FILL }
            override fun onDraw(canvas: Canvas) {
                val cx = width / 2f; val cy = height / 2f
                canvas.drawCircle(cx, cy, 16f, paint)
                canvas.drawCircle(cx, cy, 4f, center)
            }
        }
        val params = WindowManager.LayoutParams(
            dp(64), dp(64), overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        crosshairView = view
        wm.addView(view, params)
    }

    override fun onDestroy() {
        stopAutoClicker()
        fpsTracker?.stop()
        bubbleView?.let { runCatching { wm.removeView(it) } }
        panelView?.let { runCatching { wm.removeView(it) } }
        crosshairView?.let { runCatching { wm.removeView(it) } }
        clickerBubbleView?.let { runCatching { wm.removeView(it) } }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
