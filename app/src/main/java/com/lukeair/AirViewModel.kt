package com.lukeair

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lukeair.ir.AcState
import com.lukeair.ir.IrTransmitter
import com.lukeair.ir.SamsungAc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SendResult {
    data object Ok : SendResult
    data class Failed(val message: String) : SendResult
}

class AirViewModel(app: Application) : AndroidViewModel(app) {
    private val store = AcStore(app)
    private val ir = IrTransmitter(app)

    val irAvailable = ir.available

    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<AcState> = _state

    private val _sent = MutableSharedFlow<SendResult>(extraBufferCapacity = 4)
    val sent: SharedFlow<SendResult> = _sent

    private var pending: Job? = null

    fun togglePower() = apply(_state.value.copy(power = !_state.value.power), debounce = false, extended = true)

    fun setTemp(t: Int) {
        val clamped = t.coerceIn(AcState.MIN_TEMP, AcState.MAX_TEMP)
        if (clamped != _state.value.temp) apply(_state.value.copy(temp = clamped), debounce = true)
    }

    fun update(debounce: Boolean = false, change: (AcState) -> AcState) = apply(change(_state.value), debounce)

    /** Re-sends the whole state, e.g. after someone used the physical remote. */
    fun resync() = apply(_state.value, debounce = false, extended = true)

    private fun apply(next: AcState, debounce: Boolean, extended: Boolean = false) {
        _state.value = next
        store.save(next)
        // While off, settings are only remembered; turning on sends them.
        if (!next.power && !extended) return
        pending?.cancel()
        pending = viewModelScope.launch {
            if (debounce) delay(350)
            val frame = if (extended) SamsungAc.encodeExtended(next) else SamsungAc.encode(next)
            val result = withContext(Dispatchers.Default) {
                runCatching { ir.send(frame) }
            }
            _sent.emit(result.fold({ SendResult.Ok }, { SendResult.Failed(it.message ?: "Falha ao enviar") }))
        }
    }
}
