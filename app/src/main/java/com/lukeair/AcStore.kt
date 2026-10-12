package com.lukeair

import android.content.Context
import androidx.core.content.edit
import com.lukeair.ir.AcFan
import com.lukeair.ir.AcMode
import com.lukeair.ir.AcState

/** The A/C can't report back over IR, so the last state we sent is the source of truth. */
class AcStore(context: Context) {
    private val prefs = context.getSharedPreferences("ac", Context.MODE_PRIVATE)

    fun load(): AcState {
        val d = AcState()
        return AcState(
            power = prefs.getBoolean("power", false),
            mode = runCatching { AcMode.valueOf(prefs.getString("mode", null)!!) }.getOrDefault(d.mode),
            temp = prefs.getInt("temp", d.temp),
            fan = runCatching { AcFan.valueOf(prefs.getString("fan", null)!!) }.getOrDefault(d.fan),
            swing = prefs.getBoolean("swing", d.swing),
            windFree = prefs.getBoolean("windFree", d.windFree),
            display = prefs.getBoolean("display", d.display),
            quiet = prefs.getBoolean("quiet", d.quiet),
        )
    }

    fun save(s: AcState) = prefs.edit {
        putBoolean("power", s.power)
        putString("mode", s.mode.name)
        putInt("temp", s.temp)
        putString("fan", s.fan.name)
        putBoolean("swing", s.swing)
        putBoolean("windFree", s.windFree)
        putBoolean("display", s.display)
        putBoolean("quiet", s.quiet)
    }
}
