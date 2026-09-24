package os.sled.studio

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val WarmCanvas     = Color(0xFFFDF6F0)
private val WarmSurface    = Color(0xFFFFFFFF)
private val WarmCard       = Color(0xFFFFF8F2)
private val WarmAccent     = Color(0xFFC8763E)
private val WarmAccentSoft = Color(0xFFE8A87C)
private val WarmAccentGlow = Color(0x33C8763E)
private val WarmText       = Color(0xFF3B2E28)
private val WarmTextMuted  = Color(0xFF8B7A6F)
private val WarmDivider    = Color(0xFFEBE0D5)
private val WarmSuccess    = Color(0xFF6B9B37)
private val WarmWarn       = Color(0xFFD88A28)
private val WarmDanger     = Color(0xFFB83A3A)

enum class Tab(val label: String, val icon: ImageVector) {
    TRAIN  ("Обучение", Icons.Default.PlayArrow),
    MODELS ("Модели",   Icons.Default.Storage),
    HW     ("Железо",   Icons.Default.DeveloperBoard),
    FILES  ("Файлы",    Icons.Default.Folder),
    CONSOLE("Консоль",  Icons.Default.Terminal)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SledStudioTheme { SledStudioApp() } }
    }
}

private fun safeNativeVersion(): String = try {
    NativeBridge.nativeVersion().toString()
} catch (t: Throwable) {
    Log.e("SLED", "nativeVersion failed", t)
    "unavailable"
}

private fun safeBfmmla(): String = try {
    NativeBridge.nativeBfmmlaTest().toString()
} catch (t: Throwable) {
    Log.e("SLED", "bfmmla failed", t)
    "ERR: ${t.message}"
}

@Composable
fun SledStudioTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = WarmAccent,
        onPrimary = Color.White,
        primaryContainer = WarmAccentSoft,
        onPrimaryContainer = WarmText,
        background = WarmCanvas,
        onBackground = WarmText,
        surface = WarmSurface,
        onSurface = WarmText,
        surfaceVariant = WarmCard,
        onSurfaceVariant = WarmTextMuted,
        outline = WarmDivider,
        error = WarmDanger
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun SledStudioApp() {
    var tab by remember { mutableStateOf(Tab.TRAIN) }
    Scaffold(
        containerColor = WarmCanvas,
        bottomBar = {
            NavigationBar(containerColor = WarmSurface, tonalElevation = 6.dp) {
                Tab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WarmAccent,
                            selectedTextColor = WarmAccent,
                            unselectedIconColor = WarmTextMuted,
                            unselectedTextColor = WarmTextMuted,
                            indicatorColor = WarmAccentGlow
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.TRAIN   -> TrainingScreen()
                Tab.MODELS  -> ModelsScreen()
                Tab.HW      -> HardwareScreen()
                Tab.FILES   -> FilesScreen()
                Tab.CONSOLE -> ConsoleScreen()
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WarmText)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontSize = 12.sp, color = WarmTextMuted)
    }
}

@Composable
fun WarmCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = WarmSurface,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 2.dp
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp, letterSpacing = 1.sp,
        fontWeight = FontWeight.SemiBold, color = WarmTextMuted,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun MetricTile(label: String, value: String, tint: Color = WarmText) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(WarmCard)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .fillMaxWidth()
    ) {
        Text(label, fontSize = 11.sp, color = WarmTextMuted)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

@Composable
fun PrimaryButton(text: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WarmAccent, contentColor = Color.White,
            disabledContainerColor = WarmDivider, disabledContentColor = WarmTextMuted
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun GhostButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmText),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun WarmSlider(
    label: String, value: Float, range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String, onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = WarmTextMuted)
            Text(format(value), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WarmAccent)
        }
        Slider(
            value = value, onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = WarmAccent, activeTrackColor = WarmAccent,
                inactiveTrackColor = WarmDivider
            )
        )
    }
}

@Composable
fun TrainingScreen() {
    val scroll = rememberScrollState()
    var isTraining by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf("config_gpt_144m.json") }
    var lr by remember { mutableFloatStateOf(3e-4f) }
    var batch by remember { mutableFloatStateOf(8f) }
    var seqLen by remember { mutableFloatStateOf(512f) }
    var wd by remember { mutableFloatStateOf(0.1f) }
    val losses = remember {
        mutableStateListOf(7.85f, 7.21f, 6.72f, 6.34f, 6.05f, 5.81f, 5.62f, 5.47f, 5.35f, 5.24f, 5.16f, 5.09f)
    }

    LaunchedEffect(isTraining) {
        while (isTraining) {
            delay(900)
            losses.add((losses.last() - 0.04f - Math.random().toFloat() * 0.05f).coerceAtLeast(2f))
            if (losses.size > 60) losses.removeAt(0)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        ScreenHeader("Управление обучением", "SLED-Studio · PySe C-core · ARM64")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WarmCard {
                SectionLabel("Конфигурация модели")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(config, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WarmText)
                        Text("144M · d_model=768 · 12 layers", fontSize = 11.sp, color = WarmTextMuted)
                    }
                    Icon(Icons.Default.ExpandMore, contentDescription = null, tint = WarmTextMuted)
                }
            }
            WarmCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Loss")
                    Text(String.format("%.3f", losses.last()), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WarmAccent)
                }
                LossChart(losses = losses, modifier = Modifier.fillMaxWidth().height(140.dp).padding(top = 4.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { MetricTile("step/s", if (isTraining) "2.29" else "—") }
                Box(Modifier.weight(1f)) { MetricTile("RSS", "2.7 GB") }
                Box(Modifier.weight(1f)) { MetricTile("CPU", if (isTraining) "62°C" else "38°C", if (isTraining) WarmWarn else WarmText) }
            }
            WarmCard {
                SectionLabel("Гиперпараметры")
                WarmSlider("Learning rate", lr, 1e-5f..1e-3f, { String.format("%.1e", it) }) { lr = it }
                WarmSlider("Batch size", batch, 1f..64f, { "${it.toInt()}" }) { batch = it }
                WarmSlider("Seq length", seqLen, 128f..2048f, { "${it.toInt()}" }) { seqLen = it }
                WarmSlider("Weight decay", wd, 0f..0.5f, { String.format("%.2f", it) }) { wd = it }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isTraining) PrimaryButton("Старт", Icons.Default.PlayArrow) { isTraining = true }
                else GhostButton("Пауза", Icons.Default.Pause) { isTraining = false }
                GhostButton("Стоп", Icons.Default.Stop) { isTraining = false }
                GhostButton("Resume", Icons.Default.Refresh) { }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun LossChart(losses: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (losses.size < 2) return@Canvas
        val minV = losses.min()
        val maxV = losses.max()
        val range = (maxV - minV).coerceAtLeast(0.001f)
        for (i in 0..4) {
            val y = h * i / 4f
            drawLine(WarmDivider, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        for (i in 0..5) {
            val x = w * i / 5f
            drawLine(WarmDivider.copy(alpha = 0.4f), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }
        val fill = Path().apply {
            val dx = w / (losses.size - 1)
            moveTo(0f, h)
            losses.forEachIndexed { i, v -> lineTo(i * dx, h - ((v - minV) / range) * (h - 8f) - 4f) }
            lineTo(w, h); close()
        }
        drawPath(fill, WarmAccentGlow)
        val path = Path().apply {
            val dx = w / (losses.size - 1)
            losses.forEachIndexed { i, v ->
                val x = i * dx
                val y = h - ((v - minV) / range) * (h - 8f) - 4f
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(path, WarmAccent, style = Stroke(width = 3f, cap = StrokeCap.Round))
        val lastX = w
        val lastY = h - ((losses.last() - minV) / range) * (h - 8f) - 4f
        drawCircle(WarmAccent, radius = 5f, center = Offset(lastX, lastY))
        drawCircle(Color.White, radius = 2f, center = Offset(lastX, lastY))
    }
}

data class ModelFile(val name: String, val size: String, val loaded: Boolean)

@Composable
fun ModelsScreen() {
    val files = remember {
        mutableStateListOf(
            ModelFile("gpt_30m.stf", "118 MB", false),
            ModelFile("gpt_144m.stf", "576 MB", true),
            ModelFile("gpt_300m.stf", "1.2 GB", false),
            ModelFile("tokenizer.bpe", "512 KB", false)
        )
    }
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Модели", "STF-чекпоинты и инференс")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WarmCard {
                SectionLabel("Файлы моделей")
                files.forEachIndexed { idx, f ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { files[idx] = f.copy(loaded = !f.loaded) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (f.loaded) WarmAccentGlow else WarmCard),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Memory, contentDescription = null,
                                tint = if (f.loaded) WarmAccent else WarmTextMuted,
                                modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WarmText)
                            Text(f.size, fontSize = 11.sp, color = WarmTextMuted)
                        }
                        if (f.loaded) {
                            Box(Modifier.clip(RoundedCornerShape(8.dp))
                                .background(WarmSuccess.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("в RAM", fontSize = 10.sp, color = WarmSuccess, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (idx < files.lastIndex)
                        Divider(color = WarmDivider, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            WarmCard {
                SectionLabel("Инференс")
                OutlinedTextField(
                    value = prompt, onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Введите промпт…", color = WarmTextMuted, fontSize = 14.sp) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmAccent, unfocusedBorderColor = WarmDivider,
                        cursorColor = WarmAccent,
                        focusedContainerColor = WarmCanvas, unfocusedContainerColor = WarmCanvas,
                        focusedTextColor = WarmText, unfocusedTextColor = WarmText
                    ),
                    maxLines = 4
                )
                Spacer(Modifier.height(10.dp))
                PrimaryButton("Сгенерировать", Icons.Default.Send) {
                    answer = "«…» — ответ модели появится после подключения JNI."
                }
                if (answer.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(WarmCanvas).padding(14.dp)) {
                        Text(answer, fontSize = 14.sp, color = WarmText, lineHeight = 20.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun HardwareScreen() {
    var threads by remember { mutableFloatStateOf(8f) }
    var governor by remember { mutableStateOf("schedutil") }
    var bfmmlaResult by remember { mutableStateOf<String?>(null) }
    val caps = listOf(
        Triple("BFMMLA", true, "bf16 matmul"),
        Triple("i8mm", true, "int8 matmul"),
        Triple("fp16", true, "half precision"),
        Triple("SVE2", false, "Scalable Vector"),
        Triple("DOTPROD", true, "int8 dot product")
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Железо", "Snapdragon 8 Gen 3 · ARMv8.6-A")
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WarmCard {
                SectionLabel("Тест C-ядра")
                Text(
                    bfmmlaResult ?: "Нажмите, чтобы проверить BFMMLA через JNI",
                    fontSize = 13.sp, color = if (bfmmlaResult == null) WarmTextMuted else WarmText,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(10.dp))
                PrimaryButton("Test C-core", Icons.Default.PlayArrow) { bfmmlaResult = safeBfmmla() }
            }
            WarmCard {
                SectionLabel("Потоки OpenMP")
                WarmSlider("Threads", threads, 1f..8f, { "${it.toInt()}" }) { threads = it }
            }
            WarmCard {
                SectionLabel("HWCAP")
                caps.forEachIndexed { idx, (name, ok, desc) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(CircleShape)
                            .background(if (ok) WarmSuccess.copy(alpha = 0.15f) else WarmDivider),
                            contentAlignment = Alignment.Center) {
                            Icon(if (ok) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (ok) WarmSuccess else WarmTextMuted,
                                modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WarmText, fontFamily = FontFamily.Monospace)
                            Text(desc, fontSize = 11.sp, color = WarmTextMuted)
                        }
                    }
                    if (idx < caps.lastIndex)
                        Divider(color = WarmDivider, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            WarmCard {
                SectionLabel("Governor CPU")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(governor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = WarmText, fontFamily = FontFamily.Monospace)
                    Icon(Icons.Default.ExpandMore, contentDescription = null, tint = WarmTextMuted)
                }
            }
            WarmCard {
                SectionLabel("Thermal")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Thermostat, contentDescription = null, tint = WarmWarn, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Троттлинг: не обнаружен", fontSize = 14.sp, color = WarmText)
                        Text("CPU 38°C · GPU 34°C", fontSize = 11.sp, color = WarmTextMuted)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

data class DataFile(val name: String, val size: String, val kind: String)

@Composable
fun FilesScreen() {
    val files = listOf(
        DataFile("tokens_ru_en.bin", "1.8 GB", "STF"),
        DataFile("tokenizer.bpe", "512 KB", "BPE"),
        DataFile("corpus.jsonl", "2.4 GB", "JSONL"),
        DataFile("val_split.bin", "120 MB", "STF"),
        DataFile("train.log", "8.4 MB", "LOG")
    )
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Файлы", "pyse/data/")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Импорт", Icons.Default.Upload) { }
            GhostButton("Токенизация", Icons.Default.Science) { }
            GhostButton("Обновить", Icons.Default.Refresh) { }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files) { f ->
                Surface(color = WarmSurface, shape = RoundedCornerShape(14.dp), shadowElevation = 1.dp) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(WarmCard),
                            contentAlignment = Alignment.Center) {
                            Text(f.kind, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WarmAccent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WarmText, fontFamily = FontFamily.Monospace)
                            Text(f.size, fontSize = 11.sp, color = WarmTextMuted)
                        }
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = WarmTextMuted)
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun ConsoleScreen() {
    val lines = remember {
        mutableStateListOf(
            "[boot] SLED-Studio v0.1.0-mvp",
            "[jni ] nativeVersion() → ${safeNativeVersion()}",
            "[pyse] model: 144M params, 12 layers",
            "[pyse] bfmmla: enabled (armv8.6-a)",
            "[train] step=0 loss=7.850 step/s=—",
            "[train] step=1 loss=7.210 step/s=2.29",
            "[train] step=2 loss=6.720 step/s=2.31"
        )
    }
    var cmd by remember { mutableStateOf("") }
    val scroll = rememberScrollState()
    LaunchedEffect(lines.size) { scroll.animateScrollTo(scroll.maxValue) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Консоль", "stdout / stderr · live")
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            color = WarmSurface, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(14.dp)) {
                lines.forEach { line ->
                    val color = when {
                        line.startsWith("[train]") -> WarmAccent
                        line.startsWith("[jni") -> WarmTextMuted
                        line.startsWith("[pyse]") -> WarmSuccess
                        else -> WarmText
                    }
                    Text(line, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = color, lineHeight = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            color = WarmSurface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            shadowElevation = 4.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding().imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$ ", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = WarmAccent, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = cmd, onValueChange = { cmd = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("pyse_cmd…", color = WarmTextMuted, fontSize = 13.sp, fontFamily = FontFamily.Monospace) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmAccent, unfocusedBorderColor = WarmDivider,
                        cursorColor = WarmAccent,
                        focusedContainerColor = WarmCanvas, unfocusedContainerColor = WarmCanvas,
                        focusedTextColor = WarmText, unfocusedTextColor = WarmText
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (cmd.isNotBlank()) {
                            lines.add("> $cmd")
                            lines.add("[stub] команда принята (JNI не подключён)")
                            cmd = ""
                        }
                    },
                    enabled = cmd.isNotBlank(),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = WarmAccent, contentColor = Color.White,
                        disabledContainerColor = WarmDivider
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Выполнить", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}