package com.tomtune.audio

import com.tomtune.contracts.*
import kotlin.math.*

/** Feasibility parameters only: calibrate on physical toms before product use. */
data class AnalysisParameters(
    val minHz: Double = 80.0,
    val maxHz: Double = 700.0,
    val onsetRms: Double = 0.018,
    val noiseRatio: Double = 5.0,
    val analysisSeconds: Double = 0.32,
    val rearmSeconds: Double = 0.40,
    val clippingLevel: Float = 0.985f,
    val clippingFraction: Double = 0.005,
)

/** Streaming mono analyzer; call from one analysis worker, never the render thread. No target bias. */
class TomStrikeAnalyzer(private val parameters: AnalysisParameters = AnalysisParameters()) : StrikeAnalyzer {
    private var rate = 0
    private var nextFrame = 0L
    private var id = 0L
    private var onsetFrame = 0L
    private var lastOnset = Long.MIN_VALUE / 2
    private var envelope = 0.0
    private var noise = 0.001
    private var armed = true
    private var frame = ArrayList<Float>()
    private var pending: ArrayList<Float>? = null

    override fun reset() {
        rate = 0; nextFrame = 0; id = 0; onsetFrame = 0; lastOnset = Long.MIN_VALUE / 2
        envelope = 0.0; noise = 0.001; armed = true; frame.clear(); pending = null
    }

    override fun accept(block: PcmBlock): List<StrikeEvent> {
        require(block.sampleRate in 8000..192000) { "Unsupported PCM rate" }
        require(block.samples.all { it.isFinite() }) { "PCM must be finite" }
        if (rate != 0 && (rate != block.sampleRate || nextFrame != block.startFrame)) reset()
        if (rate == 0) { rate = block.sampleRate; nextFrame = block.startFrame }
        val events = ArrayList<StrikeEvent>()
        val hop = (rate * 0.005).roundToInt()
        for (sample in block.samples) {
            frame.add(sample)
            pending?.add(sample)
            nextFrame++
            if (frame.size >= hop) {
                val rms = sqrt(frame.sumOf { it.toDouble() * it } / frame.size)
                val since = (nextFrame - lastOnset).toDouble() / rate
                if (!armed && pending == null && since >= parameters.rearmSeconds && rms < 0.010) armed = true
                if (armed && pending == null && rms > parameters.onsetRms && rms > noise * parameters.noiseRatio && rms > envelope * 2.2) {
                    id++; onsetFrame = nextFrame - frame.size; lastOnset = onsetFrame
                    pending = ArrayList<Float>().also { it.addAll(frame) }
                    armed = false
                    events.add(StrikeEvent.Detected(id, nextFrame.toDouble() / rate))
                }
                if (rms < parameters.onsetRms && pending == null) noise = noise * 0.97 + rms * 0.03
                envelope = max(rms, envelope * 0.88)
                frame.clear()
            }
            val capture = pending
            if (capture != null && capture.size >= (rate * parameters.analysisSeconds).toInt()) {
                events.add(estimate(capture, nextFrame.toDouble() / rate))
                pending = null
            }
        }
        return events
    }

    override fun finish(): List<StrikeEvent> {
        val capture = pending ?: return emptyList()
        pending = null
        return listOf(if (capture.size >= rate * 0.28) estimate(capture, nextFrame.toDouble() / rate)
        else StrikeEvent.Rejected(id, nextFrame.toDouble() / rate, RejectionReason.TooShort))
    }

    private data class Peak(val hz: Double, val power: Double)
    private data class Spectrum(val peaks: List<Peak>, val concentration: Double)
    private fun estimate(samples: List<Float>, time: Double): StrikeEvent {
        fun reject(reason: RejectionReason) = StrikeEvent.Rejected(id, time, reason)
        if (samples.count { abs(it) >= parameters.clippingLevel }.toDouble() / samples.size > parameters.clippingFraction) return reject(RejectionReason.Clipped)
        val windows = listOf(0.020, 0.084, 0.148).map { spectrum(samples, it) }
        if (windows.any { it.peaks.isEmpty() || it.concentration < 0.45 }) return reject(RejectionReason.Noise)
        val first = windows.first().peaks
        val tracks = first.mapNotNull { peak ->
            val matches = windows.map { window -> window.peaks.minByOrNull { abs(1200 * log2(it.hz / peak.hz)) } }
            if (matches.any { it == null || abs(1200 * log2(it.hz / peak.hz)) > 35 }) null
            else matches.filterNotNull()
        }.filter { track -> track.last().power >= windows.last().peaks.maxOf { it.power } * 0.035 }
        if (tracks.isEmpty()) return reject(RejectionReason.Noise)
        val byLatePower = tracks.sortedByDescending { it.last().power }
        var winner = byLatePower.first()
        if (byLatePower.size > 1) {
            // A resolved lower component plus an exact octave supports a lower period, even
            // when the octave is stronger. Never invent a subharmonic without a spectral peak.
            val lower = byLatePower.minBy { it.first().hz }
            val harmonicFamily = byLatePower.all { track ->
                val ratio = track.first().hz / lower.first().hz
                abs(1200 * log2(ratio / ratio.roundToInt().coerceAtLeast(1))) < 20
            }
            if (harmonicFamily) winner = lower
            else {
                // A faster dying transient resonance is distinguishable from a long-lived mode.
                val persistence = byLatePower.sortedByDescending { it.last().power / it.first().power }
                val best = persistence[0]
                val other = persistence[1]
                val advantage = (best.last().power / best.first().power) / (other.last().power / other.first().power)
                if (advantage < 2.5) return reject(RejectionReason.AmbiguousModes)
                winner = best
            }
        }
        val frequencies = winner.map { it.hz }.sorted()
        val hz = frequencies[frequencies.size / 2]
        return StrikeEvent.Estimate(id, time, hz, windows.minOf { it.concentration }.coerceIn(0.0, 1.0))
    }

    private fun spectrum(samples: List<Float>, start: Double): Spectrum {
        // 128 ms Hann windows resampled to 8 kHz; 4096-point zero-padded FFT.
        val size = 4096
        val re = DoubleArray(size); val im = DoubleArray(size)
        var signalPower = 0.0
        // Low-pass before decimation: prevent high capture-rate partials folding into the band.
        val radius = ceil(rate / 8000.0 * 8).toInt()
        val cutoff = 1600.0 / rate
        val filter = DoubleArray(radius * 2 + 1) { tap ->
            val x = tap - radius
            val sinc = if (x == 0) 2 * cutoff else sin(2 * PI * cutoff * x) / (PI * x)
            sinc * (0.54 + 0.46 * cos(PI * x / radius))
        }
        val filterSum = filter.sum()
        for (i in 0 until 1024) {
            val position = (start + i / 8000.0) * rate
            val j = position.toInt()
            val fraction = position - j
            var filtered = 0.0
            for (tap in filter.indices) {
                val at = j + tap - radius
                val a = samples.getOrElse(at) { 0f }.toDouble()
                val b = samples.getOrElse(at + 1) { 0f }.toDouble()
                filtered += (a + (b - a) * fraction) * filter[tap]
            }
            re[i] = filtered / filterSum * (0.5 - 0.5 * cos(2 * PI * i / 1023))
            signalPower += re[i] * re[i]
        }
        if (signalPower < 0.00001) return Spectrum(emptyList(), 0.0)
        fft(re, im)
        val power = DoubleArray(size / 2) { re[it] * re[it] + im[it] * im[it] }
        val lo = (parameters.minHz * size / 8000).toInt().coerceAtLeast(2)
        val hi = (parameters.maxHz * size / 8000).toInt().coerceAtMost(power.size - 2)
        val maximum = (lo..hi).maxOf { power[it] }
        val local = (lo..hi).filter { power[it] > power[it - 1] && power[it] >= power[it + 1] && power[it] > maximum * 0.035 }
        val peaks = local.sortedByDescending { power[it] }.take(6).map { bin ->
            val left = ln(power[bin - 1].coerceAtLeast(1e-30)); val middle = ln(power[bin].coerceAtLeast(1e-30)); val right = ln(power[bin + 1].coerceAtLeast(1e-30))
            val shift = (0.5 * (left - right) / (left - 2 * middle + right)).coerceIn(-0.5, 0.5)
            Peak((bin + shift) * 8000 / size, power[bin])
        }
        val covered = BooleanArray(power.size)
        local.sortedByDescending { power[it] }.take(6).forEach { bin -> for (j in (bin - 4).coerceAtLeast(0)..(bin + 4).coerceAtMost(power.lastIndex)) covered[j] = true }
        val total = power.sum()
        val concentration = power.indices.filter { covered[it] }.sumOf { power[it] } / total.coerceAtLeast(1e-20)
        return Spectrum(peaks, concentration)
    }

    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size; var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { val a = re[i]; re[i] = re[j]; re[j] = a }
        }
        var length = 2
        while (length <= n) {
            val angle = -2 * PI / length
            val wr = cos(angle); val wi = sin(angle)
            var offset = 0
            while (offset < n) {
                var ur = 1.0; var ui = 0.0
                for (k in 0 until length / 2) {
                    val a = offset + k; val b = a + length / 2
                    val vr = re[b] * ur - im[b] * ui; val vi = re[b] * ui + im[b] * ur
                    re[b] = re[a] - vr; im[b] = im[a] - vi; re[a] += vr; im[a] += vi
                    val next = ur * wr - ui * wi; ui = ur * wi + ui * wr; ur = next
                }
                offset += length
            }
            length *= 2
        }
    }
}
