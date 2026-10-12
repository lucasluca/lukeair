package com.lukeair.ir

import android.content.Context
import android.hardware.ConsumerIrManager

class IrTransmitter(context: Context) {
    private val ir: ConsumerIrManager? = context.getSystemService(ConsumerIrManager::class.java)

    val available: Boolean get() = ir?.hasIrEmitter() == true

    /** Blocks for the duration of the signal (~150–300 ms); call off the main thread. */
    fun send(frame: ByteArray) {
        val manager = ir?.takeIf { it.hasIrEmitter() } ?: error("Este celular não tem emissor IR")
        manager.transmit(SamsungAc.CARRIER_HZ, SamsungAc.timings(frame))
    }
}
