package com.lukeair.ir

/**
 * Samsung split A/C IR protocol (remotes DB93-xxxx / DB96-xxxx, WindFree line).
 *
 * Ported from IRremoteESP8266 `ir_Samsung.cpp` (crankyoldgit). Every message
 * carries the whole A/C state, so the phone never has to know what the unit
 * is currently doing: sending the state we want is enough.
 *
 * A standard message is 2 sections of 7 bytes. Power on/off needs the
 * "extended" 3-section message (21 bytes) with a fixed middle section.
 */
enum class AcMode(val code: Int) { AUTO(0), COOL(1), DRY(2), FAN(3), HEAT(4) }

enum class AcFan(val code: Int) { AUTO(0), LOW(2), MED(4), HIGH(5), TURBO(7) }

data class AcState(
    val power: Boolean = true,
    val mode: AcMode = AcMode.COOL,
    val temp: Int = 23,
    val fan: AcFan = AcFan.AUTO,
    val swing: Boolean = false,
    val windFree: Boolean = false,
    val display: Boolean = true,
    val quiet: Boolean = false,
) {
    companion object {
        const val MIN_TEMP = 16
        const val MAX_TEMP = 30
    }
}

object SamsungAc {
    const val CARRIER_HZ = 38_000

    private const val HDR_MARK = 690
    private const val HDR_SPACE = 17_844
    private const val SECTION_MARK = 3_086
    private const val SECTION_SPACE = 8_864
    private const val SECTION_GAP = 2_886
    private const val BIT_MARK = 586
    private const val ONE_SPACE = 1_432
    private const val ZERO_SPACE = 436

    private const val SECTION = 7

    private const val SWING_V = 0b010
    private const val SWING_OFF = 0b111
    private const val SPECIAL_OFF = 0b000
    private const val SPECIAL_POWERFUL = 0b011
    private const val SPECIAL_WINDFREE = 0b101
    private const val FAN_AUTO_IN_AUTO_MODE = 6

    private val BASE = intArrayOf(
        0x02, 0x92, 0x0F, 0x00, 0x00, 0x00, 0xF0,
        0x01, 0x02, 0xAE, 0x71, 0x00, 0x15, 0xF0,
    )
    private val EXTENDED_MIDDLE = intArrayOf(0x01, 0xD2, 0x0F, 0x00, 0x00, 0x00, 0x00)

    /** Standard 14-byte settings message. */
    fun encode(s: AcState): ByteArray {
        val b = BASE.copyOf()
        val temp = s.temp.coerceIn(AcState.MIN_TEMP, AcState.MAX_TEMP) - AcState.MIN_TEMP
        val power = if (s.power) 0b11 else 0b00

        val turbo = s.fan == AcFan.TURBO
        val windFree = s.windFree && !turbo
        val fan = when {
            s.mode == AcMode.AUTO -> FAN_AUTO_IN_AUTO_MODE
            windFree || s.quiet -> AcFan.AUTO.code
            else -> s.fan.code
        }
        val special = when {
            windFree -> SPECIAL_WINDFREE
            turbo && s.mode != AcMode.AUTO -> SPECIAL_POWERFUL
            else -> SPECIAL_OFF
        }
        val swing = if (s.swing && !windFree) SWING_V else SWING_OFF
        val quiet = s.quiet && !turbo

        b[5] = b[5].setBits(5, 1, if (quiet) 1 else 0)
        b[6] = b[6].setBits(4, 2, power)
        b[9] = b[9].setBits(4, 3, swing)
        b[10] = b[10].setBits(1, 3, special).setBits(4, 1, if (s.display) 1 else 0)
        b[11] = b[11].setBits(4, 4, temp)
        b[12] = b[12].setBits(1, 3, fan).setBits(4, 3, s.mode.code)
        b[13] = b[13].setBits(4, 2, power)

        fixChecksums(b)
        return ByteArray(b.size) { b[it].toByte() }
    }

    /** 21-byte message required when switching the unit on or off. */
    fun encodeExtended(s: AcState): ByteArray {
        val std = encode(s).map { it.toInt() and 0xFF }
        val b = (std.subList(0, SECTION) + EXTENDED_MIDDLE.toList() + std.subList(SECTION, 2 * SECTION))
            .toIntArray()
        fixChecksums(b)
        return ByteArray(b.size) { b[it].toByte() }
    }

    /** Mark/space durations in microseconds for [android.hardware.ConsumerIrManager.transmit]. */
    fun timings(bytes: ByteArray): IntArray {
        require(bytes.size % SECTION == 0)
        val out = ArrayList<Int>(4 + bytes.size * 16 + 8)
        out += HDR_MARK
        out += HDR_SPACE
        for (offset in bytes.indices step SECTION) {
            out += SECTION_MARK
            out += SECTION_SPACE
            for (i in offset until offset + SECTION) {
                val v = bytes[i].toInt()
                for (bit in 0 until 8) { // LSB first
                    out += BIT_MARK
                    out += if ((v shr bit) and 1 == 1) ONE_SPACE else ZERO_SPACE
                }
            }
            out += BIT_MARK
            out += SECTION_GAP
        }
        out.removeAt(out.lastIndex) // the pattern must not end on a trailing gap
        return out.toIntArray()
    }

    fun sectionChecksum(b: IntArray, offset: Int): Int {
        var sum = Integer.bitCount(b[offset])
        sum += Integer.bitCount(b[offset + 1] and 0x0F)
        sum += Integer.bitCount(b[offset + 2] and 0xF0)
        for (i in 3 until SECTION) sum += Integer.bitCount(b[offset + i])
        return sum xor 0xFF
    }

    private fun fixChecksums(b: IntArray) {
        for (offset in b.indices step SECTION) {
            val cs = sectionChecksum(b, offset)
            b[offset + 1] = (b[offset + 1] and 0x0F) or ((cs and 0x0F) shl 4)
            b[offset + 2] = (b[offset + 2] and 0xF0) or (cs shr 4)
        }
    }

    private fun Int.setBits(offset: Int, size: Int, value: Int): Int {
        val mask = ((1 shl size) - 1) shl offset
        return (this and mask.inv()) or ((value shl offset) and mask)
    }
}
