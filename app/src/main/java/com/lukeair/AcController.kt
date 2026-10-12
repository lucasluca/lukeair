package com.lukeair

import android.content.Context
import com.lukeair.ir.AcState
import com.lukeair.ir.IrTransmitter
import com.lukeair.ir.SamsungAc
import com.lukeair.widget.AirWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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

/**
 * Single owner of the A/C state for the whole process, shared by the app screen and the
 * home-screen widget so both always show (and send) the same thing.
 */
class AcController private constructor(private val context: Context) {
    private val store = AcStore(context)
    private val ir = IrTransmitter(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val irAvailable = ir.available

    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<AcState> = _state

    private val _sent = MutableSharedFlow<SendResult>(extraBufferCapacity = 4)
    val sent: SharedFlow<SendResult> = _sent

    private var pending: Job? = null

    /** Stores [next], refreshes widgets and sends it. Returns the send job, if any. */
    fun apply(next: AcState, debounce: Boolean = false, extended: Boolean = false): Job? {
        _state.value = next
        store.save(next)
        AirWidget.updateAll(context, next)
        // While off, settings are only remembered; turning on sends them.
        if (!next.power && !extended) return null
        pending?.cancel()
        return scope.launch {
            if (debounce) delay(350)
            val frame = if (extended) SamsungAc.encodeExtended(next) else SamsungAc.encode(next)
            val result = withContext(Dispatchers.IO) { runCatching { ir.send(frame) } }
            _sent.emit(result.fold({ SendResult.Ok }, { SendResult.Failed(it.message ?: "Falha ao enviar") }))
        }.also { pending = it }
    }

    fun togglePower() = apply(_state.value.copy(power = !_state.value.power), extended = true)

    fun update(debounce: Boolean = false, change: (AcState) -> AcState) = apply(change(_state.value), debounce)

    companion object {
        @Volatile private var instance: AcController? = null

        fun get(context: Context): AcController =
            instance ?: synchronized(this) {
                instance ?: AcController(context.applicationContext).also { instance = it }
            }
    }
}
