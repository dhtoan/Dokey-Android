@file:OptIn(ExperimentalMaterial3Api::class)

package com.dhtoan.dokey.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.dhtoan.dokey.BuildConfig
import com.dhtoan.dokey.DokeySettings
import com.dhtoan.dokey.ime.KeyboardTheme
import com.dhtoan.dokey.ime.Themes

class SettingsActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DokeyTheme { SettingsScreen(tick.intValue) } }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue++
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // The keyboard picker is a dialog over this window; refresh the setup status when it closes.
        if (hasFocus) tick.intValue++
    }
}

@Composable
private fun DokeyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFF8AB4F8))
        else -> lightColorScheme(primary = Color(0xFF1E6FE8))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private fun isEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    return imm.enabledInputMethodList.any { it.packageName == context.packageName }
}

private fun isSelected(context: Context): Boolean {
    val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return false
    return id.startsWith(context.packageName + "/")
}

/** A preference backed by Compose state, so the screen redraws when it changes. */
private class Pref<T>(private val state: MutableState<T>, private val save: (T) -> Unit) {
    var value: T
        get() = state.value
        set(v) {
            state.value = v
            save(v)
        }
}

@Composable
private fun boolPref(p: SharedPreferences, key: String, def: Boolean) =
    remember { Pref(mutableStateOf(p.getBoolean(key, def))) { v -> p.edit { putBoolean(key, v) } } }

@Composable
private fun stringPref(p: SharedPreferences, key: String, def: String) =
    remember { Pref(mutableStateOf(p.getString(key, def) ?: def)) { v -> p.edit { putString(key, v) } } }

@Composable
private fun intPref(p: SharedPreferences, key: String, def: Int) =
    remember { Pref(mutableStateOf(p.getInt(key, def))) { v -> p.edit { putInt(key, v) } } }

@Composable
private fun SettingsScreen(tick: Int) {
    val context = LocalContext.current
    val settings = remember { DokeySettings(context) }
    val p = settings.prefs
    val enabled = remember(tick) { isEnabled(context) }
    val selected = remember(tick) { isSelected(context) }

    val method = stringPref(p, DokeySettings.METHOD, "telex")
    val modern = boolPref(p, DokeySettings.MODERN_TONE, true)
    val spell = boolPref(p, DokeySettings.SPELL_CHECK, true)
    val autoCap = boolPref(p, DokeySettings.AUTO_CAP, true)
    val doubleSpace = boolPref(p, DokeySettings.DOUBLE_SPACE, true)
    val theme = stringPref(p, DokeySettings.THEME, DokeySettings.defaultTheme)
    val keyHeight = stringPref(p, DokeySettings.KEY_HEIGHT, "medium")
    val borders = boolPref(p, DokeySettings.KEY_BORDERS, true)
    val preview = boolPref(p, DokeySettings.KEY_PREVIEW, true)
    val numberRow = boolPref(p, DokeySettings.NUMBER_ROW, settings.numberRow)
    val oneHand = stringPref(p, DokeySettings.ONE_HAND, "off")
    val vibrate = boolPref(p, DokeySettings.VIBRATE, true)
    val vibrateMs = intPref(p, DokeySettings.VIBRATE_MS, 12)
    val sound = boolPref(p, DokeySettings.SOUND, false)
    var testText by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Dokey", fontWeight = FontWeight.SemiBold) }) }) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = inner.calculateTopPadding() + 8.dp,
                bottom = inner.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!enabled || !selected) {
                item {
                    Section("Bắt đầu") {
                        SetupStep(
                            "1. Bật Dokey trong danh sách bàn phím", enabled, "Mở cài đặt",
                        ) { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
                        SetupStep("2. Chọn Dokey làm bàn phím đang dùng", selected, "Chọn bàn phím") {
                            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                                .showInputMethodPicker()
                        }
                    }
                }
            }

            item {
                Section("Gõ tiếng Việt") {
                    Text("Kiểu gõ", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp, 8.dp))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(method.value == "telex", { method.value = "telex" }, label = { Text("Telex") })
                        FilterChip(method.value == "vni", { method.value = "vni" }, label = { Text("VNI") })
                    }
                    SwitchRow("Bỏ dấu kiểu mới", if (modern.value) "hoà, thuý, khoẻ" else "hòa, thúy, khỏe", modern.value) {
                        modern.value = it
                    }
                    SwitchRow(
                        "Giữ nguyên từ tiếng Anh", "Không thêm dấu vào từ không phải tiếng Việt (class, google…)",
                        spell.value,
                    ) { spell.value = it }
                    SwitchRow("Tự viết hoa đầu câu", null, autoCap.value) { autoCap.value = it }
                    SwitchRow("Chấm câu bằng hai lần dấu cách", null, doubleSpace.value) { doubleSpace.value = it }
                }
            }

            item {
                Section("Giao diện") {
                    Text("Chủ đề", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp, 8.dp))
                    ThemeGallery(theme.value) { theme.value = it }
                    Text("Chiều cao phím", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((id, label) in listOf("small" to "Thấp", "medium" to "Vừa", "large" to "Cao")) {
                            FilterChip(keyHeight.value == id, { keyHeight.value = id }, label = { Text(label) })
                        }
                    }
                    Text("Chế độ một tay", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((id, label) in listOf("off" to "Tắt", "left" to "Tay trái", "right" to "Tay phải")) {
                            FilterChip(oneHand.value == id, { oneHand.value = id }, label = { Text(label) })
                        }
                    }
                    SwitchRow("Viền phím", null, borders.value) { borders.value = it }
                    SwitchRow("Phóng to phím khi nhấn", null, preview.value) { preview.value = it }
                    SwitchRow("Hàng số", "Nên bật khi gõ VNI", numberRow.value) { numberRow.value = it }
                }
            }

            item {
                Section("Rung và âm thanh") {
                    SwitchRow("Rung khi gõ", null, vibrate.value) { vibrate.value = it }
                    if (vibrate.value) {
                        Text(
                            "Độ rung: ${vibrateMs.value} ms", style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Slider(
                            value = vibrateMs.value.toFloat(),
                            onValueChange = { vibrateMs.value = it.toInt() },
                            valueRange = 5f..40f,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    SwitchRow("Âm thanh khi gõ", null, sound.value) { sound.value = it }
                }
            }

            item {
                Section("Thử bàn phím") {
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        label = { Text("Gõ thử ở đây") },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }

            item {
                Text(
                    "Dokey ${BuildConfig.VERSION_NAME} · bộ gõ viết riêng cho Dokey bằng Kotlin, chạy hoàn toàn trên máy, " +
                        "không gửi dữ liệu đi đâu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) { content() }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@Composable
private fun SetupStep(title: String, done: Boolean, action: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(if (done) "Đã xong" else "Chưa xong") },
        trailingContent = {
            if (done) OutlinedButton(onClick = onClick) { Text(action) } else Button(onClick = onClick) { Text(action) }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun ThemeGallery(selected: String, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val entries = buildList {
        if (Build.VERSION.SDK_INT >= 31) add(Themes.MATERIAL_YOU to "Material You")
        add(Themes.SYSTEM to "Theo hệ thống")
        Themes.PRESETS.forEach { add(it.id to it.name) }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(entries, key = { it.first }) { (id, name) ->
            val t = remember(id) { Themes.resolve(context, id) }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(id) },
            ) {
                ThemePreview(t, id == selected)
                Spacer(Modifier.height(6.dp))
                Text(
                    name, style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (id == selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

/** A miniature keyboard in the theme's colours. */
@Composable
private fun ThemePreview(t: KeyboardTheme, selected: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            .width(112.dp)
            .height(76.dp)
            .clip(shape)
            .border(if (selected) 3.dp else 1.dp, ring, shape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.linearGradient(listOf(Color(t.bgTop), Color(t.bgBottom))))
            val pad = 6.dp.toPx()
            val gap = 3.dp.toPx()
            val rows = 3
            val keyH = (size.height - pad * 2 - gap * (rows - 1)) / rows
            val r = CornerRadius(3.dp.toPx())
            for (row in 0 until rows) {
                val y = pad + row * (keyH + gap)
                if (row == rows - 1) {
                    val unit = (size.width - pad * 2) / 10
                    drawRoundRect(Color(t.keyFn), Offset(pad, y), Size(unit * 2 - gap, keyH), r)
                    drawRoundRect(Color(t.key), Offset(pad + unit * 2, y), Size(unit * 6 - gap, keyH), r)
                    drawRoundRect(Color(t.accent), Offset(pad + unit * 8, y), Size(unit * 2, keyH), r)
                } else {
                    val n = 6
                    val w = (size.width - pad * 2 - gap * (n - 1)) / n
                    for (i in 0 until n) drawRoundRect(Color(t.key), Offset(pad + i * (w + gap), y), Size(w, keyH), r)
                }
            }
        }
    }
}
