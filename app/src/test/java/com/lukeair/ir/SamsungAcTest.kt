package com.lukeair.ir

import org.junit.Assert.assertEquals
import org.junit.Test

/** Expected frames are real remote captures quoted in IRremoteESP8266's tests. */
class SamsungAcTest {
    private fun hex(b: ByteArray) = b.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

    private val cool = AcState(power = true, mode = AcMode.COOL, temp = 16, fan = AcFan.AUTO, swing = false)

    @Test fun coolAuto16() = assertEquals(
        "02 92 0F 00 00 00 F0 01 F2 FE 71 00 11 F0",
        hex(SamsungAc.encode(cool)),
    )

    @Test fun coolAuto20() = assertEquals(
        "02 92 0F 00 00 00 F0 01 E2 FE 71 40 11 F0",
        hex(SamsungAc.encode(cool.copy(temp = 20))),
    )

    @Test fun powerful() = assertEquals(
        "02 92 0F 00 00 00 F0 01 A2 FE 77 00 1F F0",
        hex(SamsungAc.encode(cool.copy(fan = AcFan.TURBO))),
    )

    @Test fun extendedOn() = assertEquals(
        "02 92 0F 00 00 00 F0 01 D2 0F 00 00 00 00 01 E2 FE 71 80 11 F0",
        hex(SamsungAc.encodeExtended(cool.copy(temp = 24))),
    )

    @Test fun extendedOff() = assertEquals(
        "02 B2 0F 00 00 00 C0 01 D2 0F 00 00 00 00 01 02 FF 71 80 11 C0",
        hex(SamsungAc.encodeExtended(cool.copy(temp = 24, power = false))),
    )

    @Test fun displayOffClearsLightBit() {
        val b = SamsungAc.encode(cool.copy(display = false))
        assertEquals(0x61, b[10].toInt() and 0xFF)
    }

    @Test fun timingsShape() {
        val t = SamsungAc.timings(SamsungAc.encode(cool))
        // header(2) + per section: mark/space(2) + 56 bits*2 + footer mark/gap(2), minus trailing gap
        assertEquals(2 + 2 * (2 + 112 + 2) - 1, t.size)
        assertEquals(690, t[0])
    }
}
