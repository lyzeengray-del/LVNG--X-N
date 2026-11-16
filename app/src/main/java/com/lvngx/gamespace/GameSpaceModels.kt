package com.lvngx.gamespace

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class GameItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null
)

enum class PerfMode(val title: String, val desc: String) {
    ECO("Eco", "Hemat baterai & suhu dingin"),
    BALANCED("Balanced", "Seimbang stabil"),
    ULTRA("Ultra Boost", "Maksimal FPS & Bersihkan RAM")
}

object GameSpaceStore {
    private const val PREF_NAME = "lvngx_zayon_store"
    private const val KEY_GAMES = "saved_games"
    private const val KEY_PERF_MODE = "perf_mode"
    private const val KEY_CROSSHAIR = "crosshair_enabled"
    private const val KEY_AUDIO_BOOST = "audio_boost"
    private const val KEY_CLICK_CPS = "click_cps"
    private const val KEY_CLICK_X = "click_x"
    private const val KEY_CLICK_Y = "click_y"

    private val gson = Gson()

    fun getSavedGamePackages(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GAMES, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<String>>(raw, object : TypeToken<List<String>>() {}.type)
        }.getOrDefault(emptyList())
    }

    fun saveGamePackages(context: Context, packages: List<String>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_GAMES, gson.toJson(packages.distinct())).apply()
    }

    fun addGamePackage(context: Context, pkg: String) {
        val current = getSavedGamePackages(context).toMutableList()
        if (!current.contains(pkg)) {
            current.add(pkg)
            saveGamePackages(context, current)
        }
    }

    fun removeGamePackage(context: Context, pkg: String) {
        val current = getSavedGamePackages(context).toMutableList()
        current.remove(pkg)
        saveGamePackages(context, current)
    }

    fun getPerfMode(context: Context): PerfMode {
        val name = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PERF_MODE, PerfMode.ULTRA.name) ?: PerfMode.ULTRA.name
        return runCatching { PerfMode.valueOf(name) }.getOrDefault(PerfMode.ULTRA)
    }

    fun setPerfMode(context: Context, mode: PerfMode) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PERF_MODE, mode.name).apply()
    }

    fun isCrosshairEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_CROSSHAIR, false)

    fun setCrosshairEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CROSSHAIR, enabled).apply()
    }

    fun isAudioBoost(context: Context): Boolean =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_AUDIO_BOOST, false)

    fun setAudioBoost(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUDIO_BOOST, enabled).apply()
    }

    fun getClickCps(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_CLICK_CPS, 10)

    fun setClickCps(context: Context, cps: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_CLICK_CPS, cps.coerceIn(1, 50)).apply()
    }

    fun getClickCoord(context: Context): Pair<Float, Float> {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val x = sp.getFloat(KEY_CLICK_X, 540f)
        val y = sp.getFloat(KEY_CLICK_Y, 960f)
        return x to y
    }

    fun setClickCoord(context: Context, x: Float, y: Float) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_CLICK_X, x).putFloat(KEY_CLICK_Y, y).apply()
    }

    fun loadInstalledLaunchableApps(context: Context): List<GameItem> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(mainIntent, 0)
            .mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = ri.loadLabel(pm)?.toString() ?: pkg
                val icon = runCatching { ri.loadIcon(pm) }.getOrNull()
                GameItem(pkg, label, icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    fun resolveSavedGames(context: Context): List<GameItem> {
        val pm = context.packageManager
        return getSavedGamePackages(context).mapNotNull { pkg ->
            runCatching {
                val appInfo: ApplicationInfo = pm.getApplicationInfo(pkg, 0)
                GameItem(
                    packageName = pkg,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            }.getOrNull()
        }
    }
}
