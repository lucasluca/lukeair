package com.lukeair.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lukeair.AirViewModel
import com.lukeair.SendResult
import com.lukeair.ir.AcFan
import com.lukeair.ir.AcMode
import com.lukeair.ir.AcState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Bg = Color(0xFF0B1020)
private val Surface = Color(0xFF151B2E)
private val SurfaceHi = Color(0xFF1E2640)
private val TextHi = Color(0xFFF1F5F9)
private val TextLo = Color(0xFF8C96B0)
private val OffGrey = Color(0xFF5B6478)

/**
 * Layout follows thumb reach: what is only read (temperature, status) sits on top,
 * occasional settings in the middle, and the most used controls — mode, temperature
 * slider, display and power — in the bottom third, where the thumb rests.
 */
@Composable
fun AirScreen(vm: AirViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    fun tick() = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

    val info = s.mode.info()
    val accent by animateColorAsState(if (s.power) info.color else OffGrey, tween(500), label = "accent")
    val glow by animateColorAsState(if (s.power) info.glow else OffGrey, tween(500), label = "glow")

    var flash by remember { mutableStateOf<SendResult?>(null) }
    var flashId by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { vm.sent.collect { flash = it; flashId++ } }
    LaunchedEffect(flashId) {
        if (flash != null) { delay(if (flash is SendResult.Ok) 900 else 3500); flash = null }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Header(accent, flash, onResync = { tick(); vm.resync() })

            if (!vm.irAvailable) Banner("Este aparelho não tem emissor infravermelho.")

            Hero(s, accent, glow, Modifier.weight(1f))

            val controls = Modifier.alpha(if (s.power) 1f else 0.38f)

            SettingsCard(s, accent, controls,
                onFan = { f -> tick(); vm.update { it.copy(fan = f, windFree = false, quiet = false) } },
                onWindFree = { tick(); vm.update { it.copy(windFree = !it.windFree, quiet = false, swing = if (!it.windFree) false else it.swing) } },
                onSwing = { tick(); vm.update { it.copy(swing = !it.swing, windFree = false) } },
                onQuiet = { tick(); vm.update { it.copy(quiet = !it.quiet, windFree = false, fan = if (it.fan == AcFan.TURBO) AcFan.AUTO else it.fan) } },
            )

            ModeRow(s.mode, s.power, controls) { m -> tick(); vm.update { it.copy(mode = m) } }

            TempSlider(
                temp = s.temp,
                enabled = s.power,
                accent = accent,
                modifier = controls,
                onTemp = { t -> tick(); vm.setTemp(t) },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                DisplayTile(s.display, s.power, accent, controls.weight(1f)) {
                    tick(); vm.update { it.copy(display = !it.display) }
                }
                PowerButton(s.power, accent, glow) { tick(); vm.togglePower() }
            }
        }
    }
}

@Composable
private fun Header(accent: Color, flash: SendResult?, onResync: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("LukeAir", color = TextHi, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.height(18.dp)) {
                AnimatedContent(
                    targetState = flash,
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(300)) },
                    label = "status",
                ) { f ->
                    when (f) {
                        null -> Text("Samsung WindFree", color = TextLo, fontSize = 12.sp)
                        SendResult.Ok -> Text("● Sinal enviado", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        is SendResult.Failed -> Text("⚠ ${f.message}", color = Color(0xFFF87171), fontSize = 12.sp)
                    }
                }
            }
        }
        // Rarely needed, so it lives in the hard-to-reach top corner.
        PressScale(onClick = onResync) { mod ->
            Box(mod.size(40.dp).clip(CircleShape).background(Surface), contentAlignment = Alignment.Center) {
                GlyphIcon(Glyph.SYNC, TextLo, size = 18.dp)
            }
        }
    }
}

@Composable
private fun Banner(text: String) {
    Text(
        text, color = Color(0xFFFECACA), fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x33F87171)).padding(12.dp),
    )
}

/** Read-only temperature display with a slowly drifting aura in the mode's colours. */
@Composable
private fun Hero(s: AcState, accent: Color, glow: Color, modifier: Modifier) {
    val auraAlpha = animateFloatAsState(if (s.power) 1f else 0.25f, tween(700), label = "auraA")

    BoxWithConstraints(modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(Surface), contentAlignment = Alignment.Center) {
        val h = maxHeight
        if (s.power) {
            val t = rememberInfiniteTransition(label = "aura")
            val phase = t.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "phase")
            val breath = t.animateFloat(0.85f, 1.1f, infiniteRepeatable(tween(4_000), RepeatMode.Reverse), label = "breath")
            Aura(accent, glow, { phase.value }, { breath.value }, { auraAlpha.value })
        } else {
            Aura(accent, glow, { 0f }, { 1f }, { auraAlpha.value })
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                (if (s.power) s.mode.info().doing else "Desligado").uppercase(),
                color = if (s.power) accent else TextLo,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            val numSp = with(LocalDensity.current) { (h * 0.5f).coerceAtMost(180.dp).toSp() }
            AnimatedContent(
                targetState = s.temp,
                transitionSpec = {
                    val up = targetState > initialState
                    (slideInVertically(spring(0.7f, 400f)) { if (up) it / 3 else -it / 3 } + fadeIn(tween(160))) togetherWith
                        (slideOutVertically(spring(0.7f, 400f)) { if (up) -it / 3 else it / 3 } + fadeOut(tween(120)))
                },
                label = "temp",
            ) { temp ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "$temp",
                        style = TextStyle(
                            brush = if (s.power) Brush.verticalGradient(listOf(TextHi, accent)) else Brush.verticalGradient(listOf(TextLo, OffGrey)),
                            fontSize = numSp, lineHeight = numSp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-6).sp,
                        ),
                    )
                    Text("°", color = accent, fontSize = numSp * 0.4f, fontWeight = FontWeight.Light)
                }
            }
        }
    }
}

/** Animated values arrive as lambdas and are read while drawing, so the aura never recomposes. */
@Composable
private fun Aura(accent: Color, glow: Color, phase: () -> Float, breath: () -> Float, alpha: () -> Float) {
    Canvas(Modifier.fillMaxSize()) {
        val p = phase()
        val k = alpha()
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension * 0.55f * breath()
        val a = Offset(cos(p) * size.width * 0.18f, sin(p) * size.height * 0.12f)
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.42f * k), Color.Transparent), c + a, r), r, c + a)
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.32f * k), Color.Transparent), c - a, r * 0.9f), r * 0.9f, c - a)
    }
}

@Composable
private fun SettingsCard(
    s: AcState, accent: Color, modifier: Modifier,
    onFan: (AcFan) -> Unit, onWindFree: () -> Unit, onSwing: () -> Unit, onQuiet: () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Surface).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FanRow(s, accent, onFan)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("WindFree", Glyph.WIND, s.windFree, accent, s.power, Modifier.weight(1f), onWindFree)
            Chip("Swing", Glyph.SWING, s.swing && !s.windFree, accent, s.power, Modifier.weight(1f), onSwing)
            Chip("Silencioso", Glyph.MOON, s.quiet, accent, s.power, Modifier.weight(1f), onQuiet)
        }
    }
}

@Composable
private fun FanRow(s: AcState, accent: Color, onSelect: (AcFan) -> Unit) {
    val locked = s.mode == AcMode.AUTO || s.windFree || s.quiet
    val labels = listOf(AcFan.AUTO to "Auto", AcFan.LOW to "1", AcFan.MED to "2", AcFan.HIGH to "3", AcFan.TURBO to "Turbo")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        GlyphIcon(Glyph.FAN, TextLo, Modifier.padding(horizontal = 8.dp), size = 18.dp)
        labels.forEach { (f, label) ->
            val sel = if (locked) f == AcFan.AUTO else s.fan == f
            val bg by animateColorAsState(if (sel) accent.copy(alpha = 0.22f) else Color.Transparent, label = "fbg")
            Box(
                Modifier
                    .weight(if (f == AcFan.AUTO || f == AcFan.TURBO) 1.3f else 1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable(enabled = s.power && !locked) { onSelect(f) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (sel) accent else if (locked) OffGrey else TextHi, fontSize = 14.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun Chip(label: String, glyph: Glyph, on: Boolean, accent: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) accent.copy(alpha = 0.18f) else SurfaceHi.copy(alpha = 0.6f), label = "cbg")
    PressScale(onClick = onClick, enabled = enabled, modifier = modifier) { mod ->
        Row(
            mod.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(bg)
                .border(1.dp, if (on) accent.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(14.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphIcon(glyph, if (on) accent else TextLo, size = 18.dp)
            Spacer(Modifier.width(6.dp))
            Text(label, color = if (on) TextHi else TextLo, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

/** Mode selector: a springy pill slides (with a little overshoot) behind the chosen mode. */
@Composable
private fun ModeRow(selected: AcMode, enabled: Boolean, modifier: Modifier, onSelect: (AcMode) -> Unit) {
    val modes = AcMode.entries
    BoxWithConstraints(modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(24.dp)).background(Surface).padding(5.dp)) {
        val cell = maxWidth / modes.size
        val x = animateDpAsState(cell * selected.ordinal, spring(0.62f, Spring.StiffnessMediumLow), label = "pillX")
        val pill by animateColorAsState(selected.info().color, tween(350), label = "pill")
        Box(Modifier.offset { IntOffset(x.value.roundToPx(), 0) }.width(cell).fillMaxHeight().clip(RoundedCornerShape(19.dp)).background(pill))
        Row(Modifier.fillMaxSize()) {
            modes.forEach { m ->
                val info = m.info()
                val sel = m == selected
                val fg by animateColorAsState(if (sel) Bg else info.color, label = "mfg")
                val tx by animateColorAsState(if (sel) Bg else TextHi, label = "mtx")
                PressScale(onClick = { onSelect(m) }, enabled = enabled, modifier = Modifier.weight(1f)) { mod ->
                    Column(mod.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        GlyphIcon(info.glyph, fg, size = 24.dp)
                        Spacer(Modifier.height(3.dp))
                        Text(info.label, color = tx, fontSize = 11.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

/**
 * Linear temperature slider. The thumb is a liquid blob (Libraries.dev Gooey "move",
 * re-done natively): its leading edge follows the finger, its trailing edge catches up
 * on a softer spring, so it stretches while moving and wobbles back into a circle.
 */
@Composable
private fun TempSlider(temp: Int, enabled: Boolean, accent: Color, modifier: Modifier, onTemp: (Int) -> Unit) {
    val onTempNow by rememberUpdatedState(onTemp)
    val tempNow by rememberUpdatedState(temp)
    val enabledNow by rememberUpdatedState(enabled)
    Row(modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StepButton(Glyph.MINUS, accent, enabled && temp > AcState.MIN_TEMP) { onTemp(temp - 1) }
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(Surface)) {
            val density = LocalDensity.current
            val w = constraints.maxWidth.toFloat()
            val r = with(density) { 24.dp.toPx() }
            val pad = with(density) { 8.dp.toPx() }
            val range = (AcState.MAX_TEMP - AcState.MIN_TEMP).toFloat()
            val minX = r + pad
            val maxX = w - r - pad
            fun xFor(t: Int) = minX + (t - AcState.MIN_TEMP) / range * (maxX - minX)
            fun tFor(x: Float) = (AcState.MIN_TEMP + ((x - minX) / (maxX - minX)).coerceIn(0f, 1f) * range).roundToInt()

            val lead = remember { Animatable(xFor(temp)) }
            val trail = remember { Animatable(xFor(temp)) }
            var dragging by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            val leadSpring = spring<Float>(0.8f, 700f)
            val trailSpring = spring<Float>(0.45f, 160f)

            fun settle(x: Float) {
                scope.launch { lead.animateTo(x, leadSpring) }
                scope.launch { trail.animateTo(x, trailSpring) }
            }
            LaunchedEffect(temp, w) { if (!dragging) settle(xFor(temp)) }

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(w) {
                        detectTapGestures { p -> if (enabledNow) tFor(p.x).let { if (it != tempNow) onTempNow(it) } }
                    }
                    .pointerInput(w) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragging = enabledNow },
                            onDragEnd = { dragging = false; settle(xFor(tempNow)) },
                            onDragCancel = { dragging = false; settle(xFor(tempNow)) },
                        ) { change, _ ->
                            if (!dragging) return@detectHorizontalDragGestures
                            val x = change.position.x.coerceIn(minX, maxX)
                            scope.launch { lead.snapTo(x) }
                            scope.launch { trail.animateTo(x, trailSpring) }
                            val t = tFor(x)
                            if (t != tempNow) onTempNow(t)
                        }
                    },
            ) {
                val cy = size.height / 2
                drawRoundRect(
                    Brush.horizontalGradient(listOf(accent.copy(alpha = 0.10f), accent.copy(alpha = 0.55f)), endX = lead.value + r),
                    topLeft = Offset(pad / 2, pad / 2),
                    size = Size(lead.value + r, size.height - pad),
                    cornerRadius = CornerRadius((size.height - pad) / 2),
                )
                for (i in 0..range.toInt()) {
                    val x = minX + i / range * (maxX - minX)
                    drawCircle(Color.White.copy(alpha = if (x < lead.value) 0.35f else 0.14f), 2.dp.toPx(), Offset(x, cy))
                }
                val left = minOf(lead.value, trail.value) - r
                val right = maxOf(lead.value, trail.value) + r
                val stretch = (abs(lead.value - trail.value) / (2 * r)).coerceAtMost(1f)
                val h = 2 * r * (1f - 0.22f * stretch)
                val halo = 4.dp.toPx()
                drawRoundRect(accent.copy(alpha = 0.35f), Offset(left - halo, cy - h / 2 - halo),
                    Size(right - left + 2 * halo, h + 2 * halo), CornerRadius(h / 2 + halo))
                drawRoundRect(Color.White, Offset(left, cy - h / 2), Size(right - left, h), CornerRadius(h / 2))
            }
            // The number rides inside the blob, at its leading edge.
            val labelW = with(density) { (2 * r).toDp() }
            Box(
                Modifier
                    .offset { IntOffset((lead.value - r).roundToInt(), 0) }
                    .width(labelW)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text("$temp", color = Bg, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        StepButton(Glyph.PLUS, accent, enabled && temp < AcState.MAX_TEMP) { onTemp(temp + 1) }
    }
}

@Composable
private fun StepButton(glyph: Glyph, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    PressScale(onClick = onClick, enabled = enabled) { mod ->
        Box(
            mod.size(56.dp).clip(CircleShape)
                .background(if (enabled) accent.copy(alpha = 0.16f) else SurfaceHi)
                .border(1.5.dp, if (enabled) accent.copy(alpha = 0.5f) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) { GlyphIcon(glyph, if (enabled) accent else OffGrey, size = 24.dp) }
    }
}

@Composable
private fun DisplayTile(on: Boolean, enabled: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Surface else Color(0xFF070A14), label = "dbg")
    PressScale(onClick = onClick, enabled = enabled, modifier = modifier) { mod ->
        Row(
            mod.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(26.dp)).background(bg)
                .border(1.dp, if (on) accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (on) accent.copy(alpha = 0.18f) else SurfaceHi),
                contentAlignment = Alignment.Center,
            ) { GlyphIcon(Glyph.DISPLAY, if (on) accent else TextLo, size = 24.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Display", color = TextHi, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(if (on) "Painel aceso" else "Painel apagado", color = TextLo, fontSize = 12.sp)
            }
            Switch(on, accent)
        }
    }
}

/**
 * Power is the screen's primary action, so it gets the one highlight effect:
 * Libraries.dev Border beam "pulse-inner", re-done natively — while the A/C runs,
 * a two-tone glow breathes inside the button.
 */
@Composable
private fun PowerButton(on: Boolean, accent: Color, glow: Color, onClick: () -> Unit) {
    val strength = animateFloatAsState(if (on) 1f else 0f, tween(600), label = "strength")
    val base by animateColorAsState(if (on) accent else SurfaceHi, label = "pwbg")
    val fg by animateColorAsState(if (on) Bg else TextHi, label = "pwfg")

    PressScale(onClick = onClick) { mod ->
        Box(mod.size(80.dp).clip(CircleShape).background(base), contentAlignment = Alignment.Center) {
            if (on || strength.value > 0f) BeamGlow(glow) { strength.value }
            GlyphIcon(Glyph.POWER, fg, size = 32.dp)
        }
    }
}

@Composable
private fun BeamGlow(glow: Color, strength: () -> Float) {
    val t = rememberInfiniteTransition(label = "beam")
    val pulse = t.animateFloat(0f, 1f, infiniteRepeatable(tween(2_200), RepeatMode.Reverse), label = "pulse")
    val spin = t.animateFloat(0f, 360f, infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "spin")
    Canvas(Modifier.fillMaxSize()) {
        val k = strength()
        val p = pulse.value
        val c = Offset(size.width / 2, size.height / 2)
        val rad = Math.toRadians(spin.value.toDouble())
        val o = Offset(cos(rad).toFloat(), sin(rad).toFloat()) * (size.width * 0.18f)
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.85f * k), Color.Transparent), c + o, size.width * (0.35f + 0.15f * p)), size.width, c)
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f * k * p), Color.Transparent), c - o, size.width * 0.4f), size.width, c)
        drawCircle(Color.White.copy(alpha = 0.35f * k), size.width / 2 - 1.5.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
private fun Switch(on: Boolean, accent: Color) {
    val x = animateFloatAsState(if (on) 1f else 0f, spring(0.6f, 500f), label = "sw")
    val track by animateColorAsState(if (on) accent else SurfaceHi, label = "swt")
    Box(Modifier.size(width = 50.dp, height = 30.dp).clip(CircleShape).background(track).padding(4.dp)) {
        Box(Modifier.offset { IntOffset((20.dp.toPx() * x.value).roundToInt(), 0) }.size(22.dp).clip(CircleShape).background(if (on) Bg else TextLo))
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
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(0.5f, 900f), label = "press")
    Box(modifier.scale(scale)) {
        content(Modifier.clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick))
    }
}
