package com.tomtune.audio

import com.tomtune.contracts.*

/** Strict RIFF PCM16 mono decoder for deterministic test injection. No device/file dependency. */
data class WavPcm(val samples: FloatArray, val sampleRate: Int) {
    fun blocks(size: Int = 480): Sequence<PcmBlock> = sequence {
        require(size > 0)
        var offset = 0
        while (offset < samples.size) {
            val end = minOf(samples.size, offset + size)
            yield(PcmBlock(samples.copyOfRange(offset, end), sampleRate, offset.toLong()))
            offset = end
        }
    }
    companion object {
        fun decode(bytes: ByteArray): WavPcm {
            fun text(at: Int, count: Int) = bytes.copyOfRange(at, at + count).decodeToString()
            fun u16(at: Int) = (bytes[at].toInt() and 255) or ((bytes[at + 1].toInt() and 255) shl 8)
            fun u32(at: Int): Int = u16(at) or (u16(at + 2) shl 16)
            require(bytes.size >= 12 && text(0, 4) == "RIFF" && text(8, 4) == "WAVE")
            var rate = 0; var data: IntRange? = null; var offset = 12
            while (offset + 8 <= bytes.size) {
                val size = u32(offset + 4)
                require(size >= 0 && size <= bytes.size - offset - 8)
                when (text(offset, 4)) {
                    "fmt " -> {
                        require(size >= 16 && u16(offset + 8) == 1 && u16(offset + 10) == 1 && u16(offset + 22) == 16) { "WAV requires PCM16 mono" }
                        rate = u32(offset + 12)
                    }
                    "data" -> data = (offset + 8) until (offset + 8 + size)
                }
                offset += 8 + size + (size and 1)
            }
            val range = requireNotNull(data) { "Missing WAV data" }
            require(rate in 8000..192000 && range.count() % 2 == 0)
            val pcm = FloatArray(range.count() / 2) { index -> u16(range.first + 2 * index).toShort() / 32768f }
            return WavPcm(pcm, rate)
        }
    }
}
