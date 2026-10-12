package com.lukeair

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lukeair.ir.AcState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class AirViewModel(app: Application) : AndroidViewModel(app) {
    private val ac = AcController.get(app)

    val irAvailable = ac.irAvailable
    val state: StateFlow<AcState> = ac.state
    val sent: SharedFlow<SendResult> = ac.sent

    fun togglePower() {
        ac.togglePower()
    }

    fun setTemp(t: Int) {
        val clamped = t.coerceIn(AcState.MIN_TEMP, AcState.MAX_TEMP)
        if (clamped != state.value.temp) ac.update(debounce = true) { it.copy(temp = clamped) }
    }

    fun update(debounce: Boolean = false, change: (AcState) -> AcState) {
        ac.update(debounce, change)
    }

    /** Re-sends the whole state, e.g. after someone used the physical remote. */
    fun resync() {
        ac.apply(state.value, extended = true)
    }
}
