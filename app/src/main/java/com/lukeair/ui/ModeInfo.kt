package com.lukeair.ui

import androidx.compose.ui.graphics.Color
import com.lukeair.ir.AcMode

internal data class ModeInfo(val label: String, val doing: String, val glyph: Glyph, val color: Color, val glow: Color)

internal fun AcMode.info() = when (this) {
    AcMode.AUTO -> ModeInfo("Auto", "Automático", Glyph.AUTO, Color(0xFFA78BFA), Color(0xFFF472B6))
    AcMode.COOL -> ModeInfo("Frio", "Resfriando", Glyph.SNOW, Color(0xFF38BDF8), Color(0xFF818CF8))
    AcMode.DRY -> ModeInfo("Seco", "Desumidificando", Glyph.DROP, Color(0xFF2DD4BF), Color(0xFF38BDF8))
    AcMode.FAN -> ModeInfo("Ventilar", "Ventilando", Glyph.FAN, Color(0xFFA3E635), Color(0xFF2DD4BF))
    AcMode.HEAT -> ModeInfo("Quente", "Aquecendo", Glyph.SUN, Color(0xFFFB923C), Color(0xFFF43F5E))
}
