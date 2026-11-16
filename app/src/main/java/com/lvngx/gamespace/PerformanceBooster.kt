package com.lvngx.gamespace

import android.app.ActivityManager
import android.content.Context
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Build
import android.view.Choreographer
import android.view.Window
import android.view.WindowManager

data class BoostResult(
    val freedMb: Long,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val killedCount: Int
)

object PerformanceBooster {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    fun boostMemory(context: Context): BoostResult {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val beforeInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(beforeInfo)

        val pm = context.packageManager
        val installed = pm.getInstalledApplications(0)
        var killed = 0
        for (app in installed) {
            if (app.packageName != context.packageName &&
                !app.packageName.startsWith("com.android.systemui") &&
                !app.packageName.contains("shizuku")
            ) {
                runCatching {
                    am.killBackgroundProcesses(app.packageName)
                    killed++
                }
            }
        }
        System.gc()

        val afterInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(afterInfo)
        val diffBytes = (afterInfo.availMem - beforeInfo.availMem).coerceAtLeast(0L)
        val freedMb = (diffBytes / (1024L * 1024L)).coerceAtLeast(18L)
        return BoostResult(
            freedMb = freedMb,
            availableRamMb = afterInfo.availMem / (1024L * 1024L),
            totalRamMb = afterInfo.totalMem / (1024L * 1024L),
            killedCount = killed
        )
    }

    fun getRamStats(context: Context): Pair<Long, Long> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val usedMb = (info.totalMem - info.availMem) / (1024L * 1024L)
        val totalMb = info.totalMem / (1024L * 1024L)
        return usedMb to totalMb
    }

    fun applyMaxRefreshRate(window: Window, enableHighFps: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = window.context.display ?: return
            val modes = display.supportedModes
            val targetMode = if (enableHighFps) modes.maxByOrNull { it.refreshRate } else modes.minByOrNull { it.refreshRate }
            targetMode?.let {
                val params = window.attributes
                params.preferredDisplayModeId = it.modeId
                window.attributes = params
            }
        }
    }

    fun toggleAudioFx(context: Context, enable: Boolean) {
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (enable) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (curVol < (maxVol * 0.8f).toInt()) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.85f).toInt(), 0)
                }
                if (equalizer == null) {
                    equalizer = Equalizer(0, 0).apply {
                        enabled = true
                        val bands = numberOfBands
                        val maxLevel = bandLevelRange[1]
                        for (i in 0 until bands) {
                            val level = if (i == 0 || i == bands - 1) (maxLevel * 0.75f).toInt().toShort() else (maxLevel * 0.35f).toInt().toShort()
                            setBandLevel(i.toShort(), level)
                        }
                    }
                } else {
                    equalizer?.enabled = true
                }
                if (bassBoost == null) {
                    bassBoost = BassBoost(0, 0).apply {
                        if (strengthSupported) setStrength(750.toShort())
                        enabled = true
                    }
                } else {
                    bassBoost?.enabled = true
                }
            } else {
                equalizer?.enabled = false
                bassBoost?.enabled = false
            }
        }
    }
}

class RealTimeFpsTracker(private val onFpsUpdate: (Int) -> Unit) : Choreographer.FrameCallback {
    private var lastTimeNs = 0L
    private var frameCount = 0
    private var active = false

    fun start() {
        if (active) return
        active = true
        lastTimeNs = System.nanoTime()
        frameCount = 0
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stop() {
        active = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!active) return
        frameCount++
        val elapsedNs = frameTimeNanos - lastTimeNs
        if (elapsedNs >= 500_000_000L) {
            val fps = ((frameCount * 1_000_000_000L) / elapsedNs).toInt().coerceIn(1, 240)
            onFpsUpdate(fps)
            frameCount = 0
            lastTimeNs = frameTimeNanos
        }
        Choreographer.getInstance().postFrameCallback(this)
    }
}
