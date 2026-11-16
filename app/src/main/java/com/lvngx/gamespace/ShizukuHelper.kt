package com.lvngx.gamespace

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuHelper {

    fun isShizukuInstalled(context: Context): Boolean {
        return runCatching {
            val pi = context.packageManager.getPackageInfo("moe.shizuku.manager", 0)
            pi != null
        }.getOrDefault(false)
    }

    fun isShizukuRunning(): Boolean {
        return runCatching {
            Shizuku.pingBinder()
        }.getOrDefault(false)
    }

    fun hasShizukuPermission(): Boolean {
        return runCatching {
            if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    fun requestPermission(requestCode: Int) {
        runCatching {
            if (!Shizuku.isPreV11() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            }
        }
    }

    fun executeShellCommand(command: String): Boolean {
        return runCatching {
            if (!isShizukuRunning()) return false
            val p = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            p.waitFor()
            p.exitValue() == 0
        }.getOrDefault(false)
    }

    fun tap(x: Float, y: Float) {
        executeShellCommand("input tap ${x.toInt()} ${y.toInt()}")
    }
}
