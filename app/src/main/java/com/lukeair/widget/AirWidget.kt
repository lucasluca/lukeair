package com.lukeair.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.lukeair.AcController
import com.lukeair.MainActivity
import com.lukeair.R
import com.lukeair.ir.AcMode
import com.lukeair.ir.AcState
import com.lukeair.ui.info
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Home-screen remote: power, −/+, mode cycle and display, sharing state with the app. */
class AirWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = render(context, AcController.get(context).state.value)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == null || !action.startsWith(PREFIX)) return super.onReceive(context, intent)

        val ac = AcController.get(context)
        val s = ac.state.value
        val job = when (action) {
            POWER -> ac.togglePower()
            // Like the app, everything but power only works while the A/C is on.
            MINUS -> if (s.power) ac.update { it.copy(temp = (it.temp - 1).coerceAtLeast(AcState.MIN_TEMP)) } else null
            PLUS -> if (s.power) ac.update { it.copy(temp = (it.temp + 1).coerceAtMost(AcState.MAX_TEMP)) } else null
            MODE -> if (s.power) ac.update { it.copy(mode = AcMode.entries[(it.mode.ordinal + 1) % AcMode.entries.size]) } else null
            DISPLAY -> if (s.power) ac.update { it.copy(display = !it.display) } else null
            else -> null
        } ?: return

        // Keep the receiver alive until the IR burst has gone out.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try { job.join() } finally { pending.finish() }
        }
    }

    companion object {
        private const val PREFIX = "com.lukeair.widget."
        private const val POWER = PREFIX + "POWER"
        private const val MINUS = PREFIX + "MINUS"
        private const val PLUS = PREFIX + "PLUS"
        private const val MODE = PREFIX + "MODE"
        private const val DISPLAY = PREFIX + "DISPLAY"

        private const val TEXT_HI = 0xFFF1F5F9.toInt()
        private const val GREY = 0xFF5B6478.toInt()
        private const val BG_DARK = 0xFF0B1020.toInt()

        fun updateAll(context: Context, state: AcState) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AirWidget::class.java))
            if (ids.isEmpty()) return
            val views = render(context, state)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun render(context: Context, s: AcState): RemoteViews {
            val info = s.mode.info()
            val accent = if (s.power) info.color.toArgb() else GREY
            return RemoteViews(context.packageName, R.layout.widget_air).apply {
                setInt(R.id.glow, "setColorFilter", accent)
                setFloat(R.id.glow, "setAlpha", if (s.power) 1f else 0.4f)

                setTextViewText(R.id.mode_label, (if (s.power) info.doing else "Desligado").uppercase())
                setTextColor(R.id.mode_label, accent)
                setTextViewText(R.id.temp, "${s.temp}°")
                setTextColor(R.id.temp, if (s.power) TEXT_HI else GREY)

                setInt(R.id.power_bg, "setColorFilter", if (s.power) accent else 0xFF1E2640.toInt())
                setInt(R.id.power_icon, "setColorFilter", if (s.power) BG_DARK else TEXT_HI)

                setFloat(R.id.controls, "setAlpha", if (s.power) 1f else 0.4f)
                setInt(R.id.minus_icon, "setColorFilter", accent)
                setInt(R.id.plus_icon, "setColorFilter", accent)
                setImageViewResource(R.id.mode_icon, modeIcon(s.mode))
                setInt(R.id.mode_icon, "setColorFilter", if (s.power) accent else GREY)
                setInt(R.id.display_icon, "setColorFilter", if (s.power && s.display) accent else GREY)

                setOnClickPendingIntent(R.id.btn_power, broadcast(context, POWER, 1))
                setOnClickPendingIntent(R.id.btn_minus, broadcast(context, MINUS, 2))
                setOnClickPendingIntent(R.id.btn_plus, broadcast(context, PLUS, 3))
                setOnClickPendingIntent(R.id.btn_mode, broadcast(context, MODE, 4))
                setOnClickPendingIntent(R.id.btn_display, broadcast(context, DISPLAY, 5))
                setOnClickPendingIntent(
                    R.id.open_app,
                    PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE),
                )
            }
        }

        private fun modeIcon(mode: AcMode) = when (mode) {
            AcMode.AUTO -> R.drawable.ic_w_auto
            AcMode.COOL -> R.drawable.ic_w_snow
            AcMode.DRY -> R.drawable.ic_w_drop
            AcMode.FAN -> R.drawable.ic_w_fan
            AcMode.HEAT -> R.drawable.ic_w_sun
        }

        private fun broadcast(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(
            context, code,
            Intent(context, AirWidget::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
