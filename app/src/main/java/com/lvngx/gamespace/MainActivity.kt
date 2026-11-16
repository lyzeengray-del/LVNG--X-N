package com.lvngx.gamespace

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val NavyBg = Color(0xFF060E20)
private val NavySurface = Color(0xFF0D1B38)
private val NavyCard = Color(0xFF12254A)
private val Crimson = Color(0xFFE91E5A)
private val CrimsonGlow = Color(0xFFFF2A6D)
private val CyanAccent = Color(0xFF38BDF8)
private val TextMuted = Color(0xFF94A3B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PerformanceBooster.applyMaxRefreshRate(window, true)
        setContent { LvngXZayonApp() }
    }
}

@Composable
fun LvngXZayonApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var savedGames by remember { mutableStateOf(GameSpaceStore.resolveSavedGames(context)) }
    var selectedGamePkg by remember { mutableStateOf(savedGames.firstOrNull()?.packageName) }
    var showAppPicker by remember { mutableStateOf(false) }

    var perfMode by remember { mutableStateOf(GameSpaceStore.getPerfMode(context)) }
    var audioFx by remember { mutableStateOf(GameSpaceStore.isAudioBoost(context)) }
    var crosshair by remember { mutableStateOf(GameSpaceStore.isCrosshairEnabled(context)) }
    var currentFps by remember { mutableIntStateOf(60) }
    var ramStats by remember { mutableStateOf(PerformanceBooster.getRamStats(context)) }

    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var shizukuRunning by remember { mutableStateOf(ShizukuHelper.isShizukuRunning()) }
    var shizukuPerm by remember { mutableStateOf(ShizukuHelper.hasShizukuPermission()) }

    DisposableEffect(Unit) {
        val tracker = RealTimeFpsTracker { fps -> currentFps = fps }
        tracker.start()
        onDispose { tracker.stop() }
    }

    LaunchedEffect(selectedTab) {
        while (true) {
            overlayGranted = Settings.canDrawOverlays(context)
            shizukuRunning = ShizukuHelper.isShizukuRunning()
            shizukuPerm = ShizukuHelper.hasShizukuPermission()
            ramStats = PerformanceBooster.getRamStats(context)
            delay(1200L)
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Crimson, secondary = CyanAccent,
            background = NavyBg, surface = NavySurface,
            onBackground = Color.White, onSurface = Color.White
        )
    ) {
        Scaffold(
            containerColor = NavyBg,
            bottomBar = {
                NavigationBar(containerColor = NavySurface, tonalElevation = 8.dp) {
                    NavigationBarItem(
                        selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.SportsEsports, null) },
                        label = { Text("Mulai Game", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, selectedTextColor = CrimsonGlow, indicatorColor = Crimson)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.FlashOn, null) },
                        label = { Text("Auto-Clicker", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, selectedTextColor = CrimsonGlow, indicatorColor = Crimson)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2, onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Tune, null) },
                        label = { Text("Fitur Menu", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, selectedTextColor = CrimsonGlow, indicatorColor = Crimson)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3, onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Security, null) },
                        label = { Text("Setup Shizuku", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, selectedTextColor = CrimsonGlow, indicatorColor = Crimson)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF0B1C3D), NavyBg)))
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardTab(
                        savedGames = savedGames,
                        selectedGamePkg = selectedGamePkg,
                        onSelectGame = { selectedGamePkg = it },
                        onOpenPicker = { showAppPicker = true },
                        currentFps = currentFps,
                        ramStats = ramStats,
                        shizukuReady = shizukuRunning && shizukuPerm,
                        overlayGranted = overlayGranted,
                        onGoSetup = { selectedTab = 3 },
                        onLaunchGame = {
                            if (!Settings.canDrawOverlays(context)) {
                                Toast.makeText(context, "Izinkan Jendela Mengambang (Overlay) dulu!", Toast.LENGTH_SHORT).show()
                                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                            } else if (!ShizukuHelper.isShizukuRunning() || !ShizukuHelper.hasShizukuPermission()) {
                                Toast.makeText(context, "Shizuku belum aktif/izin belum diberikan!", Toast.LENGTH_LONG).show()
                                selectedTab = 3
                            } else {
                                PerformanceBooster.boostMemory(context)
                                (context as? Activity)?.window?.let { PerformanceBooster.applyMaxRefreshRate(it, true) }

                                val svc = Intent(context, FloatingPanelService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc)
                                else context.startService(svc)

                                Toast.makeText(context, "⚡ Zayon Floating Panel Aktif!", Toast.LENGTH_SHORT).show()

                                selectedGamePkg?.let { pkg ->
                                    context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(it)
                                    }
                                }
                            }
                        }
                    )

                    1 -> AutoClickerTab()

                    2 -> FeaturesTab(
                        perfMode = perfMode,
                        onChangePerf = { mode ->
                            perfMode = mode
                            GameSpaceStore.setPerfMode(context, mode)
                            (context as? Activity)?.window?.let { PerformanceBooster.applyMaxRefreshRate(it, mode != PerfMode.ECO) }
                        },
                        audioFx = audioFx,
                        onToggleAudio = { audioFx = it; GameSpaceStore.setAudioBoost(context, it); PerformanceBooster.toggleAudioFx(context, it) },
                        crosshair = crosshair,
                        onToggleCrosshair = { crosshair = it; GameSpaceStore.setCrosshairEnabled(context, it) },
                        currentFps = currentFps,
                        ramStats = ramStats
                    )

                    3 -> SetupShizukuTab(shizukuRunning, shizukuPerm)
                }

                if (showAppPicker) {
                    GamePickerDialog(
                        onDismiss = { showAppPicker = false },
                        onPick = { pkg ->
                            GameSpaceStore.addGamePackage(context, pkg)
                            savedGames = GameSpaceStore.resolveSavedGames(context)
                            selectedGamePkg = pkg
                            showAppPicker = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTab(
    savedGames: List<GameItem>,
    selectedGamePkg: String?,
    onSelectGame: (String) -> Unit,
    onOpenPicker: () -> Unit,
    currentFps: Int,
    ramStats: Pair<Long, Long>,
    shizukuReady: Boolean,
    overlayGranted: Boolean,
    onGoSetup: () -> Unit,
    onLaunchGame: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.dp, if (shizukuReady && overlayGranted) CyanAccent else Crimson),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().clickable { onGoSetup() }
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("ZAYON", color = CrimsonGlow, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (shizukuReady && overlayGranted) "● Shizuku & Overlay Siap" else "⚠ Shizuku/Overlay belum aktif. Ketuk untuk setup",
                    color = if (shizukuReady && overlayGranted) CyanAccent else Color(0xFFFDA4AF),
                    fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ChevronRight, null, tint = Crimson)
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = BorderStroke(1.5.dp, Crimson),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("GAME LIBRARY", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(savedGames) { game ->
                            val isSelected = game.packageName == selectedGamePkg
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Crimson.copy(alpha = 0.3f) else NavyCard)
                                    .border(1.5.dp, if (isSelected) Crimson else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { onSelectGame(game.packageName) }
                                    .padding(6.dp)
                            ) {
                                AppIconImage(game.icon, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                                Spacer(Modifier.height(4.dp))
                                Text(game.appName, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NavyCard)
                                    .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { onOpenPicker() }
                                    .padding(6.dp)
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                Text("Tambah", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1.1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(104.dp).clip(CircleShape).background(NavyCard).border(2.dp, Crimson, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FlashOn, null, tint = CrimsonGlow, modifier = Modifier.size(54.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("ZAYON", color = Color.WHITE, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("GAME SPACE", color = CrimsonGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("FPS", "$currentFps", CyanAccent)
                        Chip("RAM", "${ramStats.first / 1024f}".take(3) + "G", CrimsonGlow)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onLaunchGame,
            colors = ButtonDefaults.buttonColors(containerColor = Crimson),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.RocketLaunch, null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text("Mulai Game & Buka Floating Zayon", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun Chip(label: String, value: String, accent: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(NavyCard).border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = TextMuted, fontSize = 9.sp)
        Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun AutoClickerTab() {
    val context = LocalContext.current
    var cps by remember { mutableStateOf(GameSpaceStore.getClickCps(context)) }
    val (x, y) = remember { GameSpaceStore.getClickCoord(context) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("AUTO-CLICKER SETTINGS", color = CrimsonGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Zayon Shizuku Speed & Target", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

        Card(colors = CardDefaults.cardColors(containerColor = NavySurface), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kecepatan Klik ($cps CPS)", color = Color.White, fontWeight = FontWeight.Bold)
                Slider(
                    value = cps.toFloat(),
                    onValueChange = {
                        cps = it.toInt()
                        GameSpaceStore.setClickCps(context, cps)
                    },
                    valueRange = 1f..50f,
                    colors = SliderDefaults.colors(thumbColor = Crimson, activeTrackColor = CyanAccent)
                )
                Text("Posisi Target Tersimpan: X=${x.toInt()}, Y=${y.toInt()}", color = TextMuted, fontSize = 12.sp)
                Text("💡 Tip: Saat dalam game, tombol bulat mengambang oranye di layar bisa digeser bebas dan ditekan untuk mulai/stop klik otomatis secara instan.", color = CyanAccent, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FeaturesTab(
    perfMode: PerfMode,
    onChangePerf: (PerfMode) -> Unit,
    audioFx: Boolean,
    onToggleAudio: (Boolean) -> Unit,
    crosshair: Boolean,
    onToggleCrosshair: (Boolean) -> Unit,
    currentFps: Int,
    ramStats: Pair<Long, Long>
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("FITUR MENU", color = CrimsonGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Boost & Sound FX", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

        Card(colors = CardDefaults.cardColors(containerColor = NavySurface), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Performance Mode", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PerfMode.entries.forEach { mode ->
                        val active = perfMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Crimson else NavyCard)
                                .clickable { onChangePerf(mode) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        FeatureCard(Icons.Default.GraphicEq, "Audio FX (Footstep Boost)", "Perjelas suara langkah kaki musuh di game", audioFx, onToggleAudio)
        FeatureCard(Icons.Default.CenterFocusStrong, "Virtual Crosshair", "Titik bidik presisi di tengah layar", crosshair, onToggleCrosshair)
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = NavySurface), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Crimson, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = TextMuted, fontSize = 11.sp)
            }
            Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Crimson))
        }
    }
}

@Composable
private fun SetupShizukuTab(running: Boolean, perm: Boolean) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SHIZUKU SETUP", color = CrimsonGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Auto-Clicker Shizuku Engine", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

        Card(colors = CardDefaults.cardColors(containerColor = NavySurface), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status Shizuku:", color = Color.White, fontWeight = FontWeight.Bold)
                Text(if (running) "🟢 Shizuku Service Running" else "🔴 Shizuku Service Not Running", color = if (running) CyanAccent else Crimson)
                Text(if (perm) "🟢 Shizuku Permission Granted" else "🔴 Shizuku Permission Missing", color = if (perm) CyanAccent else Crimson)

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { ShizukuHelper.requestPermission(1001) },
                    colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Minta Izin Shizuku", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        runCatching {
                            val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.manager")
                            if (intent != null) context.startActivity(intent)
                            else Toast.makeText(context, "Aplikasi Shizuku belum terinstal!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buka Aplikasi Shizuku", color = CyanAccent)
                }
            }
        }
    }
}

@Composable
private fun GamePickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val apps = remember { GameSpaceStore.loadInstalledLaunchableApps(context) }
    val filtered = remember(query, apps) { if (query.isBlank()) apps else apps.filter { it.appName.contains(query, ignoreCase = true) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        title = { Text("Pilih Game", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.height(340.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Cari...") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered) { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(NavyCard).clickable { onPick(app.packageName) }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(app.icon, Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)))
                            Spacer(Modifier.width(10.dp))
                            Text(app.appName, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup", color = CrimsonGlow) } }
    )
}

@Composable
private fun AppIconImage(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable == null) {
        Box(modifier.background(NavyCard), contentAlignment = Alignment.Center) { Icon(Icons.Default.SportsEsports, null, tint = Crimson) }
        return
    }
    val bitmap = remember(drawable) {
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
    }
    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = modifier)
}
