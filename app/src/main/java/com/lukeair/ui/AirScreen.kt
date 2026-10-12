package com.lukeair.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lukeair.AirViewModel
import com.lukeair.SendResult
import com.lukeair.ir.AcFan
import com.lukeair.ir.AcMode
import com.lukeair.ir.AcState
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Bg = Color(0xFF0B1020)
private val Surface = Color(0xFF151B2E)
private val SurfaceHi = Color(0xFF1E2640)
private val TextHi = Color(0xFFF1F5F9)
private val TextLo = Color(0xFF8C96B0)
private val OffGrey = Color(0xFF5B6478)

private data class ModeInfo(val label: String, val doing: String, val glyph: Glyph, val color: Color)

private fun AcMode.info() = when (this) {
    AcMode.AUTO -> ModeInfo("Auto", "Automático", Glyph.AUTO, Color(0xFFA78BFA))
    AcMode.COOL -> ModeInfo("Frio", "Resfriando", Glyph.SNOW, Color(0xFF38BDF8))
    AcMode.DRY -> ModeInfo("Seco", "Desumidificando", Glyph.DROP, Color(0xFF2DD4BF))
    AcMode.FAN -> ModeInfo("Ventilar", "Ventilando", Glyph.FAN, Color(0xFFA3E635))
    AcMode.HEAT -> ModeInfo("Quente", "Aquecendo", Glyph.SUN, Color(0xFFFB923C))
}

@Composable
fun AirScreen(vm: AirViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

    val accent by animateColorAsState(if (s.power) s.mode.info().color else OffGrey, tween(500), label = "accent")

    var flash by remember { mutableStateOf<SendResult?>(null) }
    var flashId by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        vm.sent.collect { flash = it; flashId++ }
    }
    LaunchedEffect(flashId) {
        if (flash != null) { delay(if (flash is SendResult.Ok) 900 else 3500); flash = null }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent), center = Offset(540f, 520f), radius = 1100f))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(s, accent, flash, onPower = { tick(); vm.togglePower() })

            if (!vm.irAvailable) Banner("Este aparelho não tem emissor infravermelho.")

            TempDial(
                state = s,
                accent = accent,
                onTemp = { if (s.power) { tick(); vm.setTemp(it) } },
            )

            val controls = Modifier.alpha(if (s.power) 1f else 0.38f)

            SectionLabel("Modo", controls)
            ModeRow(s.mode, s.power, controls) { m -> tick(); vm.update { it.copy(mode = m) } }

            DisplayTile(s.display, s.power, accent, controls) {
                tick(); vm.update { it.copy(display = !it.display) }
            }

            SectionLabel("Ventilação", controls)
            FanRow(s, accent, controls) { f -> tick(); vm.update { it.copy(fan = f, windFree = false, quiet = false) } }

            Row(controls, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleTile("WindFree", Glyph.WIND, s.windFree, accent, s.power, Modifier.weight(1f)) {
                    tick(); vm.update { it.copy(windFree = !it.windFree, quiet = false, swing = if (!it.windFree) false else it.swing) }
                }
                ToggleTile("Swing", Glyph.SWING, s.swing && !s.windFree, accent, s.power, Modifier.weight(1f)) {
                    tick(); vm.update { it.copy(swing = !it.swing, windFree = false) }
                }
                ToggleTile("Silencioso", Glyph.MOON, s.quiet, accent, s.power, Modifier.weight(1f)) {
                    tick(); vm.update { it.copy(quiet = !it.quiet, windFree = false, fan = if (it.fan == AcFan.TURBO) AcFan.AUTO else it.fan) }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { tick(); vm.resync() }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIcon(Glyph.SYNC, TextLo, size = 16.dp)
                Spacer(Modifier.width(8.dp))
                Text("Reenviar estado ao ar", color = TextLo, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Header(s: AcState, accent: Color, flash: SendResult?, onPower: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("LukeAir", color = TextHi, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.height(20.dp)) {
                AnimatedContent(
                    targetState = flash,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(300)) },
                    label = "status",
                ) { f ->
                    when (f) {
                        null -> Text("Samsung WindFree", color = TextLo, fontSize = 13.sp)
                        SendResult.Ok -> Text("● Sinal enviado", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        is SendResult.Failed -> Text("⚠ ${f.message}", color = Color(0xFFF87171), fontSize = 13.sp)
                    }
                }
            }
        }
        val on = s.power
        val bg by animateColorAsState(if (on) accent else SurfaceHi, label = "pbg")
        val fg by animateColorAsState(if (on) Bg else TextHi, label = "pfg")
        PressScale(onClick = onPower) { mod ->
            Box(
                mod
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) { GlyphIcon(Glyph.POWER, fg, size = 28.dp) }
        }
    }
}

@Composable
private fun Banner(text: String) {
    Text(
        text,
        color = Color(0xFFFECACA),
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33F87171))
            .padding(12.dp),
    )
}

private const val ARC_START = 135f
private const val ARC_SWEEP = 270f

@Composable
private fun TempDial(state: AcState, accent: Color, onTemp: (Int) -> Unit) {
    val range = (AcState.MAX_TEMP - AcState.MIN_TEMP).toFloat()
    val progress by animateFloatAsState((state.temp - AcState.MIN_TEMP) / range, tween(250), label = "p")
    val currentOnTemp by rememberUpdatedState(onTemp)

    fun tempAt(pos: Offset, size: androidx.compose.ui.unit.IntSize): Int? {
        val dx = pos.x - size.width / 2f
        val dy = pos.y - size.height / 2f
        var a = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        a = ((a - ARC_START) % 360f + 360f) % 360f
        if (a > ARC_SWEEP) a = if (a > ARC_SWEEP + 45f) 0f else ARC_SWEEP
        return (AcState.MIN_TEMP + a / ARC_SWEEP * range).roundToInt()
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Surface)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(270.dp)
                .pointerInput(Unit) {
                    detectTapGestures { p -> tempAt(p, size)?.let(currentOnTemp) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ -> tempAt(change.position, size)?.let(currentOnTemp) }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                val w = 16.dp.toPx()
                val inset = w / 2
                val arcSize = Size(size.width - w, size.height - w)
                val tl = Offset(inset, inset)
                drawArc(Color.White.copy(alpha = 0.07f), ARC_START, ARC_SWEEP, false, tl, arcSize, style = Stroke(w, cap = StrokeCap.Round))
                // ticks
                val r = size.width / 2
                val c = Offset(size.width / 2, size.height / 2)
                for (i in 0..14) {
                    val ang = Math.toRadians((ARC_START + i * ARC_SWEEP / 14).toDouble())
                    val dir = Offset(cos(ang).toFloat(), sin(ang).toFloat())
                    drawLine(Color.White.copy(alpha = 0.18f), c + dir * (r - w - 10.dp.toPx()), c + dir * (r - w - 4.dp.toPx()), 1.5.dp.toPx(), StrokeCap.Round)
                }
                drawArc(accent.copy(alpha = 0.25f), ARC_START, ARC_SWEEP * progress, false, tl, arcSize, style = Stroke(w * 2.0f, cap = StrokeCap.Round))
                drawArc(accent, ARC_START, ARC_SWEEP * progress, false, tl, arcSize, style = Stroke(w, cap = StrokeCap.Round))
                val knobAng = Math.toRadians((ARC_START + ARC_SWEEP * progress).toDouble())
                val knob = c + Offset(cos(knobAng).toFloat(), sin(knobAng).toFloat()) * (r - inset)
                drawCircle(Color.White, w * 0.72f, knob)
                drawCircle(accent, w * 0.38f, knob)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (state.power) state.mode.info().doing else "Desligado",
                    color = if (state.power) accent else TextLo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                AnimatedContent(
                    targetState = state.temp,
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically { if (up) it / 2 else -it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { if (up) -it / 2 else it / 2 } + fadeOut())
                    },
                    label = "temp",
                ) { t ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            "$t",
                            color = if (state.power) TextHi else TextLo,
                            fontSize = 96.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-3).sp,
                        )
                        Text("°C", color = TextLo, fontSize = 24.sp, modifier = Modifier.padding(top = 22.dp))
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundStep(Glyph.MINUS, accent, state.power && state.temp > AcState.MIN_TEMP) { onTemp(state.temp - 1) }
            Text("${AcState.MIN_TEMP}° – ${AcState.MAX_TEMP}°", color = TextLo, fontSize = 12.sp)
            RoundStep(Glyph.PLUS, accent, state.power && state.temp < AcState.MAX_TEMP) { onTemp(state.temp + 1) }
        }
    }
}

@Composable
private fun RoundStep(glyph: Glyph, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    PressScale(onClick = onClick, enabled = enabled) { mod ->
        Box(
            mod
                .size(76.dp)
                .clip(CircleShape)
                .background(if (enabled) accent.copy(alpha = 0.16f) else SurfaceHi)
                .border(1.5.dp, if (enabled) accent.copy(alpha = 0.55f) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) { GlyphIcon(glyph, if (enabled) accent else OffGrey, size = 30.dp) }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), color = TextLo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp,
        modifier = modifier.padding(start = 4.dp, bottom = 0.dp))
}

@Composable
private fun ModeRow(selected: AcMode, enabled: Boolean, modifier: Modifier, onSelect: (AcMode) -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AcMode.entries.forEach { m ->
            val info = m.info()
            val sel = m == selected
            val bg by animateColorAsState(if (sel) info.color else Surface, label = "mbg")
            val fg by animateColorAsState(if (sel) Bg else TextHi, label = "mfg")
            PressScale(onClick = { onSelect(m) }, enabled = enabled, modifier = Modifier.weight(1f)) { mod ->
                Column(
                    mod
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(bg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    GlyphIcon(info.glyph, if (sel) Bg else info.color, size = 28.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(info.label, color = fg, fontSize = 12.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun DisplayTile(on: Boolean, enabled: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Surface else Color(0xFF070A14), label = "dbg")
    PressScale(onClick = onClick, enabled = enabled, modifier = modifier) { mod ->
        Row(
            mod
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(bg)
                .border(1.dp, if (on) accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) accent.copy(alpha = 0.18f) else SurfaceHi),
                contentAlignment = Alignment.Center,
            ) { GlyphIcon(Glyph.DISPLAY, if (on) accent else TextLo, size = 30.dp) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Display", color = TextHi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(if (on) "Luzes do painel acesas" else "Painel apagado", color = TextLo, fontSize = 13.sp)
            }
            Switch(on, accent)
        }
    }
}

@Composable
private fun Switch(on: Boolean, accent: Color) {
    val x by animateFloatAsState(if (on) 1f else 0f, label = "sw")
    val track by animateColorAsState(if (on) accent else SurfaceHi, label = "swt")
    Box(
        Modifier
            .size(width = 54.dp, height = 32.dp)
            .clip(CircleShape)
            .background(track)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .padding(start = (22 * x).dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (on) Bg else TextLo)
        )
    }
}

@Composable
private fun FanRow(s: AcState, accent: Color, modifier: Modifier, onSelect: (AcFan) -> Unit) {
    val locked = s.mode == AcMode.AUTO || s.windFree || s.quiet
    val labels = listOf(AcFan.AUTO to "Auto", AcFan.LOW to "1", AcFan.MED to "2", AcFan.HIGH to "3", AcFan.TURBO to "Turbo")
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEach { (f, label) ->
            val sel = if (locked) f == AcFan.AUTO else s.fan == f
            val bg by animateColorAsState(if (sel) accent.copy(alpha = 0.22f) else Color.Transparent, label = "fbg")
            Box(
                Modifier
                    .weight(if (f == AcFan.AUTO || f == AcFan.TURBO) 1.3f else 1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable(enabled = s.power && !locked) { onSelect(f) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (sel) accent else if (locked) OffGrey else TextHi, fontSize = 14.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ToggleTile(label: String, glyph: Glyph, on: Boolean, accent: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) accent.copy(alpha = 0.18f) else Surface, label = "tbg")
    PressScale(onClick = onClick, enabled = enabled, modifier = modifier) { mod ->
        Column(
            mod
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(bg)
                .border(1.dp, if (on) accent.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(20.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlyphIcon(glyph, if (on) accent else TextLo, size = 24.dp)
            Spacer(Modifier.height(6.dp))
            Text(label, color = if (on) TextHi else TextLo, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Shrinks slightly while pressed — gives every tile a physical, remote-button feel. */
@Composable
private fun PressScale(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(120), label = "press")
    Box(modifier.scale(scale)) {
        content(Modifier.clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick))
    }
}
