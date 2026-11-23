package com.tomtune.audio

import com.tomtune.contracts.*
import kotlin.math.*
import kotlin.test.*

class TomStrikeAnalyzerTest {
    private val rate = 48000
    private fun signal(frequency: Double, secondary: Double? = null, secondaryAmplitude: Double = 0.0,
                       secondaryDecay: Double = 9.0, primaryDecay: Double = 7.0, clip: Boolean = false): FloatArray {
        return FloatArray(rate * 2) { index ->
            val t = index.toDouble() / rate - 0.2
            if (t < 0) 0f else {
                val a = 0.25 * exp(-primaryDecay * t) * sin(2 * PI * frequency * t)
                val b = if (secondary != null) secondaryAmplitude * exp(-secondaryDecay * t) * sin(2 * PI * secondary * t) else 0.0
                val value = a + b
                (if (clip) (value * 12).coerceIn(-1.0, 1.0) else value).toFloat()
            }
        }
    }
    private fun analyze(samples: FloatArray, blockSize: Int = 480): List<StrikeEvent> {
        val analyzer = TomStrikeAnalyzer()
        val events = ArrayList<StrikeEvent>()
        WavPcm(samples, rate).blocks(blockSize).forEach { events.addAll(analyzer.accept(it)) }
        events.addAll(analyzer.finish())
        return events
    }
    private fun assertFrequency(events: List<StrikeEvent>, hz: Double) {
        assertEquals(1, events.filterIsInstance<StrikeEvent.Detected>().size, events.toString())
        val estimate = events.filterIsInstance<StrikeEvent.Estimate>().single()
        val error = 1200 * log2(estimate.frequencyHz / hz)
        assertTrue(abs(error) < 12, "Expected $hz; got ${estimate.frequencyHz}, error=$error cents")
        assertEquals(1, events.count { it !is StrikeEvent.Detected })
        assertTrue(events.first() is StrikeEvent.Detected)
        assertTrue(estimate.timeSeconds > events.first().timeSeconds + 0.25)
    }
    @Test fun simpleAndDecayNotSecondStrike() = assertFrequency(analyze(signal(185.0)), 185.0)
    @Test fun dominantInharmonicTransient() = assertFrequency(analyze(signal(185.0, 311.0, 0.6, 22.0)), 185.0)
    @Test fun dominantNearOctaveTransient() = assertFrequency(analyze(signal(185.0, 378.0, 0.6, 22.0)), 185.0)
    @Test fun exactOctaveHasResolvedLowerPeriod() = assertFrequency(analyze(signal(185.0, 370.0, 0.55, 7.0)), 185.0)
    @Test fun ambiguousPersistentModesAreRejected() {
        val events = analyze(signal(185.0, 263.0, 0.30, 7.0))
        assertEquals(1, events.filterIsInstance<StrikeEvent.Detected>().size)
        assertTrue(events.filterIsInstance<StrikeEvent.Estimate>().isEmpty())
        assertEquals(RejectionReason.AmbiguousModes, events.filterIsInstance<StrikeEvent.Rejected>().single().reason)
    }
    @Test fun distantFrequencyIsNotPulledToDisplayed185HzTarget() = assertFrequency(analyze(signal(317.0)), 317.0)
    @Test fun silenceNeverDetected() { assertTrue(analyze(FloatArray(rate)).isEmpty()) }
    @Test fun deterministicBroadbandNoiseNeverAccepted() {
        var state = 1234567
        val samples = FloatArray(rate) { state = state * 1664525 + 1013904223; ((state ushr 8) / 16777216.0 * 0.3 - 0.15).toFloat() }
        assertTrue(analyze(samples).none { it is StrikeEvent.Estimate })
    }
    @Test fun clippingRejected() {
        val events = analyze(signal(185.0, clip = true))
        assertEquals(RejectionReason.Clipped, events.filterIsInstance<StrikeEvent.Rejected>().single().reason)
        assertTrue(events.none { it is StrikeEvent.Estimate })
    }
    @Test fun callbackChunkingDoesNotChangeResults() {
        val pcm = signal(185.0)
        assertEquals(analyze(pcm, 127), analyze(pcm, 1024))
    }
    @Test fun detectionPublishedBeforeEnoughDataForEstimate() {
        val analyzer = TomStrikeAnalyzer()
        val data = signal(185.0)
        val early = analyzer.accept(PcmBlock(data.copyOfRange(0, 12000), rate, 0))
        assertEquals(1, early.size)
        assertTrue(early.single() is StrikeEvent.Detected)
    }
    @Test fun rearmingAfterQuietAllowsAnotherStrike() {
        val first = signal(185.0)
        val second = signal(192.0)
        val events = analyze(first + second)
        assertEquals(2, events.filterIsInstance<StrikeEvent.Detected>().size)
        assertEquals(2, events.filterIsInstance<StrikeEvent.Estimate>().size)
        assertEquals(2, events.filterIsInstance<StrikeEvent.Estimate>().map { it.strikeId }.distinct().size)
    }
    @Test fun native44100RateIsRespected() {
        val nativeRate = 44100
        val data = FloatArray(nativeRate) { i ->
            val t = i.toDouble() / nativeRate - 0.1
            if (t < 0) 0f else (0.35 * exp(-8 * t) * sin(2 * PI * 203 * t)).toFloat()
        }
        val analyzer = TomStrikeAnalyzer()
        val events = WavPcm(data, nativeRate).blocks(441).flatMap { analyzer.accept(it).asSequence() }.toList() + analyzer.finish()
        assertFrequency(events, 203.0)
    }
    @Test fun highFrequencyDoesNotAliasIntoAcceptedBand() {
        assertTrue(analyze(signal(8185.0)).none { it is StrikeEvent.Estimate })
    }
    @Test fun noInventedSubharmonicWithoutSupportingPeak() = assertFrequency(analyze(signal(370.0)), 370.0)

    @Test fun startingOrResettingDuringDecayWaitsForQuietBeforeNextStrike() {
        val tail = FloatArray((rate * 0.65).toInt()) { i ->
            val t = i.toDouble() / rate + 0.04
            (0.30 * exp(-7 * t) * sin(2 * PI * 185 * t)).toFloat()
        }
        val analyzer = TomStrikeAnalyzer()
        // The first non-quiet hop belongs to a strike from the previous screen/selection.
        assertTrue(analyzer.accept(PcmBlock(tail, rate, 0)).isEmpty())
        analyzer.reset()
        assertTrue(analyzer.accept(PcmBlock(tail, rate, 0)).isEmpty())
        val following = FloatArray(rate / 5) + signal(192.0)
        val events = analyzer.accept(PcmBlock(following, rate, tail.size.toLong())) + analyzer.finish()
        assertFrequency(events, 192.0)
    }

}
