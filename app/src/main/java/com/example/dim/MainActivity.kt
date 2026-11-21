package com.example.dim

import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.example.dim.ui.theme.Typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val dimColors = darkColorScheme(
    background = Color(0xFF151713),
    surface = Color(0xFF20231C),
    onBackground = Color(0xFFF0F2E8),
    onSurface = Color(0xFFF0F2E8),
    onSurfaceVariant = Color(0xFFC3C9B9),
    primary = Color(0xFFD4E7A3),
    onPrimary = Color(0xFF26320E),
    outline = Color(0xFF93998B),
    outlineVariant = Color(0xFF555C4C)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bars = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        val hasLightSensor = getSystemService(SensorManager::class.java)
            .getDefaultSensor(Sensor.TYPE_LIGHT) != null
        setContent {
            MaterialTheme(colorScheme = dimColors, typography = Typography) {
                DimScreen(hasLightSensor)
            }
        }
    }

    @Composable
    private fun DimScreen(hasLightSensor: Boolean) {
        var enabled by remember { mutableStateOf(DimSettings.enabled(this)) }
        var automatic by remember { mutableStateOf(DimSettings.automatic(this)) }
        var opacity by remember { mutableIntStateOf(DimSettings.opacity(this)) }
        val connected = DimSettings.connected
        val effectiveOpacity = DimSettings.appliedOpacity
        val failed = DimSettings.overlayFailed

        DisposableEffect(Unit) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                enabled = DimSettings.enabled(this@MainActivity)
                automatic = DimSettings.automatic(this@MainActivity)
                opacity = DimSettings.opacity(this@MainActivity)
            }
            val prefs = DimSettings.preferences(this@MainActivity)
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier.windowInsetsPadding(WindowInsets.safeDrawing).fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    Modifier.widthIn(max = 520.dp).fillMaxWidth()
                        .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Dim", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        when {
                            !connected -> "Aktifkan layanan Aksesibilitas Dim untuk meredupkan layar, termasuk panel notifikasi pada perangkat yang mendukungnya."
                            failed -> "Filter gagal ditampilkan. Jeda filter lalu coba aktifkan kembali."
                            !enabled -> "Filter dijeda"
                            else -> "Filter aktif · $effectiveOpacity%"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!connected) {
                        Text("Layanan juga mengambil tangkapan layar ketika Anda mengetuk tile Tangkapan bersih.")
                        Button(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                            Text("Buka pengaturan Aksesibilitas")
                        }
                    } else {
                        Button(onClick = { DimSettings.setEnabled(this@MainActivity, !enabled) }) {
                            Text(if (enabled) "Jeda filter" else "Aktifkan filter")
                        }
                    }

                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val label = if (automatic && hasLightSensor) "Batas opasitas" else "Opasitas filter"
                            Text("$label · $opacity%", style = MaterialTheme.typography.titleMedium)
                            OpacitySlider(label, opacity, reversed = true) {
                                DimSettings.setOpacity(this@MainActivity, it)
                            }
                            Text("Pratinjau warna", style = MaterialTheme.typography.labelLarge)
                            ColorPreview(if (connected && enabled && !failed) 0 else opacity)
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Mode otomatis", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (hasLightSensor) "Sesuaikan redup dengan cahaya sekitar."
                                else "Sensor cahaya tidak tersedia di perangkat ini.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = automatic && hasLightSensor,
                            enabled = hasLightSensor,
                            onCheckedChange = { DimSettings.setAutomatic(this@MainActivity, it) },
                            modifier = Modifier.semantics { contentDescription = "Mode otomatis" }
                        )
                    }
                    HorizontalDivider()
                    Text("Setelan Cepat", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (Build.VERSION.SDK_INT >= 31) {
                            "Tambahkan Filter Dim, Dim otomatis, atau Tangkapan bersih lewat menu edit Setelan Cepat."
                        } else {
                            "Tambahkan Filter Dim atau Dim otomatis lewat menu edit Setelan Cepat. Tangkapan bersih memerlukan Android 12."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    ScreenshotRecovery()
                }
            }
        }
    }

    @Composable
    private fun ScreenshotRecovery() {
        var source by rememberSaveable { mutableStateOf<String?>(null) }
        var restorationOpacity by rememberSaveable { mutableIntStateOf(35) }
        var working by remember { mutableStateOf(false) }
        var message by rememberSaveable { mutableStateOf<String?>(null) }
        val choose = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                source = uri.toString()
                message = "Gambar dipilih."
            }
        }
        val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { destination ->
            val input = source ?: return@rememberLauncherForActivityResult
            if (destination == null) return@rememberLauncherForActivityResult
            val value = restorationOpacity
            working = true
            message = "Memulihkan gambar…"
            lifecycleScope.launch {
                val saved = withContext(Dispatchers.IO) {
                    try {
                        restoreScreenshot(contentResolver, input.toUri(), destination, value)
                    } catch (_: java.io.IOException) {
                        false
                    } catch (_: SecurityException) {
                        false
                    }
                }
                working = false
                message = if (saved) "Salinan PNG disimpan." else "Gambar gagal dipulihkan. Pilih kembali gambar sumber."
            }
        }

        Text("Pulihkan tangkapan layar", style = MaterialTheme.typography.titleMedium)
        Text(
            "Pilih gambar yang terkena filter, lalu masukkan opasitas saat gambar diambil. Warna hasil pemulihan adalah perkiraan.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = { choose.launch(arrayOf("image/*")) }, enabled = !working) {
            Text(if (source == null) "Pilih gambar" else "Ganti gambar")
        }
        if (source != null) {
            Text("Opasitas saat diambil · $restorationOpacity%")
            OpacitySlider("Opasitas saat diambil", restorationOpacity, enabled = !working) {
                restorationOpacity = it
            }
            OutlinedButton(onClick = { save.launch("Dim-dipulihkan.png") }, enabled = !working) {
                Text("Simpan salinan")
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun OpacitySlider(
    label: String,
    opacity: Int,
    enabled: Boolean = true,
    reversed: Boolean = false,
    onChange: (Int) -> Unit
) {
    fun sliderValue(value: Int) = if (reversed) MAX_OPACITY - value else value

    Slider(
        value = sliderValue(opacity).toFloat(),
        onValueChange = { onChange(sliderValue(it.roundToInt())) },
        enabled = enabled,
        valueRange = 0f..MAX_OPACITY.toFloat(),
        modifier = Modifier.semantics {
            contentDescription = label
            stateDescription = "$opacity%"
        }
    )
}

@Composable
private fun ColorPreview(opacity: Int) {
    val samples = listOf(0xFFF5F4E9, 0xFFE3BE78, 0xFFD58172, 0xFFA28ABB, 0xFF7DA8AA, 0xFF242820)
    Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(8.dp))) {
        Row(Modifier.fillMaxSize()) {
            for (sample in samples) {
                Box(Modifier.weight(1f).height(54.dp).background(Color(sample)))
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = opacity / 100f)))
    }
}
