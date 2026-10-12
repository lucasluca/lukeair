package com.lukeair.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class Glyph { POWER, SNOW, SUN, DROP, FAN, AUTO, DISPLAY, WIND, SWING, MOON, MINUS, PLUS, SYNC }

/** Line icons drawn on a 24-unit grid, so no icon font or extended icon pack is needed. */
@Composable
fun GlyphIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 24f
        val stroke = Stroke(width = 2f * u, cap = StrokeCap.Round)
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, p(x1, y1), p(x2, y2), strokeWidth = 2f * u, cap = StrokeCap.Round)

        when (glyph) {
            Glyph.POWER -> {
                drawArc(tint, -60f, 300f, false, p(4.5f, 5f), Size(15f * u, 15f * u), style = stroke)
                line(12f, 2.5f, 12f, 11f)
            }
            Glyph.SNOW -> for (i in 0 until 3) rotate(i * 60f, p(12f, 12f)) {
                line(12f, 2.5f, 12f, 21.5f)
                for (end in listOf(2.5f to 1f, 21.5f to -1f)) {
                    val (y, dir) = end
                    line(12f, y + 3f * dir, 9.2f, y + 0.6f * dir)
                    line(12f, y + 3f * dir, 14.8f, y + 0.6f * dir)
                }
            }
            Glyph.SUN -> {
                drawCircle(tint, 4.2f * u, p(12f, 12f), style = stroke)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    line(12f + 7.2f * cos(a).toFloat(), 12f + 7.2f * sin(a).toFloat(),
                        12f + 9.8f * cos(a).toFloat(), 12f + 9.8f * sin(a).toFloat())
                }
            }
            Glyph.DROP -> drawPath(Path().apply {
                moveTo(12f * u, 2.8f * u)
                cubicTo(12f * u, 2.8f * u, 5f * u, 10.5f * u, 5f * u, 14.6f * u)
                cubicTo(5f * u, 18.6f * u, 8.2f * u, 21.4f * u, 12f * u, 21.4f * u)
                cubicTo(15.8f * u, 21.4f * u, 19f * u, 18.6f * u, 19f * u, 14.6f * u)
                cubicTo(19f * u, 10.5f * u, 12f * u, 2.8f * u, 12f * u, 2.8f * u)
                close()
            }, tint, style = stroke)
            Glyph.FAN -> {
                for (i in 0 until 3) rotate(i * 120f, p(12f, 12f)) {
                    drawPath(Path().apply {
                        moveTo(12f * u, 12f * u)
                        cubicTo(9.5f * u, 9f * u, 9.5f * u, 3f * u, 12.5f * u, 2.6f * u)
                        cubicTo(16f * u, 2.4f * u, 16.5f * u, 7.5f * u, 12f * u, 12f * u)
                        close()
                    }, tint, style = stroke)
                }
                drawCircle(tint, 1.4f * u, p(12f, 12f))
            }
            Glyph.AUTO -> {
                line(6.5f, 20f, 12f, 4f); line(12f, 4f, 17.5f, 20f); line(8.6f, 14f, 15.4f, 14f)
            }
            Glyph.DISPLAY -> {
                drawRoundRect(tint, p(2.5f, 7f), Size(19f * u, 10f * u), CornerRadius(3f * u), style = stroke)
                line(7f, 12f, 7f, 12f); line(11f, 12f, 11f, 12f)
                line(14.5f, 12f, 17f, 12f)
            }
            Glyph.WIND -> {
                drawPath(Path().apply {
                    moveTo(3f * u, 9f * u); lineTo(15f * u, 9f * u)
                    arcTo(androidx.compose.ui.geometry.Rect(p(12f, 3f), Size(6f * u, 6f * u)), 90f, -270f, false)
                }, tint, style = stroke)
                drawPath(Path().apply {
                    moveTo(3f * u, 14f * u); lineTo(18f * u, 14f * u)
                    arcTo(androidx.compose.ui.geometry.Rect(p(15f, 14f), Size(6f * u, 6f * u)), -90f, 270f, false)
                }, tint, style = stroke)
                line(5f, 19f, 10f, 19f)
            }
            Glyph.SWING -> {
                drawArc(tint, 200f, 140f, false, p(3f, 6f), Size(18f * u, 18f * u), style = stroke)
                line(12f, 15f, 12f, 21f)
                line(12f, 21f, 9.5f, 18.5f); line(12f, 21f, 14.5f, 18.5f)
            }
            Glyph.MOON -> drawPath(Path().apply {
                moveTo(19.5f * u, 14.5f * u)
                cubicTo(15f * u, 16.5f * u, 8f * u, 13f * u, 9.5f * u, 4f * u)
                cubicTo(5.2f * u, 5.5f * u, 3.2f * u, 10.5f * u, 5.2f * u, 15f * u)
                cubicTo(7.5f * u, 20f * u, 15.5f * u, 21f * u, 19.5f * u, 14.5f * u)
                close()
            }, tint, style = stroke)
            Glyph.MINUS -> line(5f, 12f, 19f, 12f)
            Glyph.PLUS -> { line(5f, 12f, 19f, 12f); line(12f, 5f, 12f, 19f) }
            Glyph.SYNC -> {
                drawArc(tint, 200f, 130f, false, p(4f, 4f), Size(16f * u, 16f * u), style = stroke)
                drawArc(tint, 20f, 130f, false, p(4f, 4f), Size(16f * u, 16f * u), style = stroke)
                arrowHead(p(17.9f, 6.5f), u, tint, 1f)
                arrowHead(p(6.1f, 17.5f), u, tint, -1f)
            }
        }
    }
}

private fun DrawScope.arrowHead(at: Offset, u: Float, tint: Color, dir: Float) {
    drawLine(tint, at, at + Offset(-3.2f * u * dir, 0.2f * u * dir), 2f * u, StrokeCap.Round)
    drawLine(tint, at, at + Offset(0.3f * u * dir, 3.2f * u * dir), 2f * u, StrokeCap.Round)
}
