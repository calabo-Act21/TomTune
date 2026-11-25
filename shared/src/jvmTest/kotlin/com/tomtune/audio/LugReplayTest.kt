package com.tomtune.audio

import com.tomtune.contracts.*
import com.tomtune.tuning.lugAnalysisParameters
import kotlin.math.*
import kotlin.test.*

/** Synthetic impact timing is known; a missing detection can be measured here, unlike unknown live PCM. */
class LugReplayTest {
    private val rate = 8000
    private fun pcm(hz: Double, amplitude: Double = .25, secondHz: Double = 0.0,
                    secondAmplitude: Double = 0.0, decay: Double = 7.0, secondDecay: Double = 7.0) =
        FloatArray(rate * 2) { index ->
            val t = index.toDouble() / rate - .2
            if (t < 0) 0f else (amplitude * exp(-decay * t) * sin(2 * PI * hz * t) +
                secondAmplitude * exp(-secondDecay * t) * sin(2 * PI * secondHz * t)).toFloat()
        }
    private fun run(analyzer: StrikeAnalyzer, samples: FloatArray) =
        WavPcm(samples, rate).blocks(80).flatMap { analyzer.accept(it).asSequence() }.toList() + analyzer.finish()
    private fun report(name: String, version: String, events: List<StrikeEvent>, played: Int) {
        val detected = events.filterIsInstance<StrikeEvent.Detected>().size
        println("REPLAY case=$name version=$version expected=$played detected=$detected missed=${played-detected} " +
            "rejected=${events.filterIsInstance<StrikeEvent.Rejected>().map { it.reason }} hz=${events.filterIsInstance<StrikeEvent.Estimate>().map { it.frequencyHz }}")
    }
    @Test fun localSensitivityStillRejectsNoiseClippingAndSilence() {
        assertTrue(run(TomStrikeAnalyzer(lugAnalysisParameters()), FloatArray(rate * 2)).isEmpty())
        var state = 1234567
        val noise = FloatArray(rate * 2) { index ->
            if (index < rate / 5) 0f else {
                state = state * 1664525 + 1013904223
                ((state ushr 8) / 16777216.0 * .12 - .06).toFloat()
            }
        }
        val events = run(TomStrikeAnalyzer(lugAnalysisParameters()), noise)
        assertEquals(RejectionReason.Noise, events.filterIsInstance<StrikeEvent.Rejected>().single().reason)
        assertTrue(events.none { it is StrikeEvent.Estimate })
        val clipping = pcm(350.0, 3.0).map { it.coerceIn(-1f, 1f) }.toFloatArray()
        val clipped = run(TomStrikeAnalyzer(lugAnalysisParameters()), clipping)
        assertEquals(RejectionReason.Clipped, clipped.filterIsInstance<StrikeEvent.Rejected>().single().reason)
        assertTrue(clipped.none { it is StrikeEvent.Estimate })
    }

    @Test fun localNative48000KeepsHighModeAndRejectsAmbiguityAndDetectsWeakStrike() {
        val nativeRate = 48000
        for ((hz, amplitude, second) in listOf(Triple(700.0, .25, 0.0), Triple(350.0, .25, .35), Triple(350.0, .025, 0.0))) {
            val samples = FloatArray(nativeRate * 2) { index ->
                val t = index.toDouble() / nativeRate - .2
                if (t < 0) 0f else (exp(-7 * t) * (amplitude * sin(2 * PI * hz * t) + second * sin(2 * PI * 700 * t))).toFloat()
            }
            val analyzer = TomStrikeAnalyzer(lugAnalysisParameters())
            val events = WavPcm(samples, nativeRate).blocks(480).flatMap { analyzer.accept(it).asSequence() }.toList() + analyzer.finish()
            assertEquals(1, events.filterIsInstance<StrikeEvent.Detected>().size)
            if (second > 0) {
                assertTrue(events.none { it is StrikeEvent.Estimate })
                assertEquals(RejectionReason.AmbiguousModes, events.filterIsInstance<StrikeEvent.Rejected>().single().reason)
            } else assertEquals(hz, events.filterIsInstance<StrikeEvent.Estimate>().single().frequencyHz, .5)
        }
    }

    @Test fun samePcmAcrossLot0Lot23BaselineAndCorrection350700AndWeakStrikes() {
        val cases = listOf(
            "350" to pcm(350.0), "700" to pcm(700.0),
            "ambiguous350700" to pcm(350.0, secondHz = 700.0, secondAmplitude = .35),
            "supported350" to pcm(350.0, secondHz = 700.0, secondAmplitude = .60, secondDecay = 22.0),
            "supported700" to pcm(350.0, amplitude = .40, secondHz = 700.0, secondAmplitude = .35, decay = 14.0),
            "tenWeak" to List(10) { pcm(350.0, amplitude = .025) }.reduce { a, b -> a + b },
            "tenBelowThreshold" to List(10) { pcm(350.0, amplitude = .008) }.reduce { a, b -> a + b },
        )
        for ((name, samples) in cases) {
            val old = run(com.tomtune.audio.lot0.TomStrikeAnalyzer(), samples)
            val baseline = run(com.tomtune.audio.lot0.TomStrikeAnalyzer(
                com.tomtune.audio.lot0.AnalysisParameters(minHz = 55.0, maxHz = 900.0)), samples)
            val fixed = run(TomStrikeAnalyzer(lugAnalysisParameters()), samples)
            val played = if (name.startsWith("ten")) 10 else 1
            report(name, "lot0", old, played); report(name, "lot23baseline", baseline, played); report(name, "fixed", fixed, played)
            when (name) {
                "ambiguous350700" -> {
                    assertTrue(old.any { it is StrikeEvent.Estimate }) // expose old forced lower selection
                    assertTrue(fixed.none { it is StrikeEvent.Estimate })
                    assertEquals(RejectionReason.AmbiguousModes, fixed.filterIsInstance<StrikeEvent.Rejected>().single().reason)
                }
                "tenWeak" -> {
                    assertEquals(0, old.filterIsInstance<StrikeEvent.Detected>().size)
                    assertEquals(10, fixed.filterIsInstance<StrikeEvent.Detected>().size)
                    assertEquals(10, fixed.filterIsInstance<StrikeEvent.Estimate>().size)
                }
                "tenBelowThreshold" -> assertTrue(fixed.isEmpty())
                else -> {
                    val expected = if (name.contains("700")) 700.0 else 350.0
                    assertEquals(expected, fixed.filterIsInstance<StrikeEvent.Estimate>().single().frequencyHz, .5)
                }
            }
        }
    }
}
