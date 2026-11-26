package com.tomtune.audio

import com.tomtune.contracts.*
import com.tomtune.tuning.lugAnalysisParameters
import java.io.File
import kotlin.math.*
import kotlin.test.*

class ModeComparisonTest {
    private fun blocks(rate: Int, strokes: Int = 20) = sequence {
        repeat(strokes) { stroke ->
            repeat(200) { block ->
                yield(PcmBlock(ModeComparisonFixture.block(stroke, block * (rate / 100), rate / 100, rate),
                    rate, (stroke * rate * 2 + block * (rate / 100)).toLong()))
            }
        }
    }
    @Test fun samePcmNativeAndPairedAt48000And8000() {
        val output = System.getenv("TOMTUNE_DIAGNOSTIC_OUTPUT")?.let { File(it).apply { mkdirs() } }
        for (rate in listOf(48000, 8000)) {
            val lines = mutableListOf<String>()
            val comparison = LugDiagnosticComparison { lines.add(it) }
            val current = TomStrikeAnalyzer(lugAnalysisParameters())
            val native = mutableListOf<StrikeEvent>()
            val reference = mutableListOf<StrikeEvent>()
            var maximumBlockNanos = 0L
            for (block in blocks(rate)) {
                val start = System.nanoTime()
                native.addAll(comparison.accept(block))
                maximumBlockNanos = maxOf(maximumBlockNanos, System.nanoTime() - start)
                reference.addAll(current.accept(block))
            }
            native.addAll(comparison.finish()); reference.addAll(current.finish())
            assertEquals(reference, native, "Comparison must not change product events")
            assertEquals(20, native.filterIsInstance<StrikeEvent.Detected>().size)
            val impacts = lines.filter { it.startsWith("CMP Impact ") }
            assertEquals(20, impacts.size)
            assertEquals(60, lines.count { "phase=paired" in it && " Selection " in it })
            assertFalse(lines.any { "impact=null" in it || "Incomplete" in it })
            assertTrue(impacts.any { "lot0=missed" in it }, "Expose detector threshold difference")
            assertTrue(lines.any { "phase=paired config=lot0" in it && " hz=491." in it }, "Even a detector miss has a paired selection")
            assertTrue(lines.any { "phase=paired config=current" in it && " hz=801." in it })
            assertTrue(lines.none { "phase=paired config=band80_700" in it && " hz=801." in it })
            assertTrue(lines.any { "phase=paired" in it && "reason=AmbiguousModes" in it })
            assertTrue(lines.any { " Mode " in it && "null" in it }, "Missing peaks must be visible")
            assertTrue(lines.any { "phase=paired config=current" in it && "decay_advantage" in it })
            output?.resolve("injected-$rate.log")?.writeText("SOURCE injected rate=$rate noPhysicalClaim=true\n" + lines.joinToString("\n") + "\n")
            println("COMPARISON injected rate=$rate impacts=${impacts.size} currentDetected=${native.filterIsInstance<StrikeEvent.Detected>().size} " +
                "estimates=${native.filterIsInstance<StrikeEvent.Estimate>().size} rejected=${native.filterIsInstance<StrikeEvent.Rejected>().size} maxBlockMs=${maximumBlockNanos/1e6}")
        }
    }
    @Test fun historicalParametersMatchFrozenLot0Exactly() {
        val historical = com.tomtune.audio.lot0.TomStrikeAnalyzer()
        val instrumented = TomStrikeAnalyzer(AnalysisParameters(), diagnostic = {})
        val inputs = blocks(48000).toList() + listOf(PcmBlock(FloatArray(48000), 48000, 1920000))
        for (block in inputs) assertEquals(historical.accept(block), instrumented.accept(block))
        assertEquals(historical.finish(), instrumented.finish())
    }
    @Test fun harmonicChoiceAlsoMatchesFrozenLot0AndComparisonDoesNotBiasProduct() {
        val samples = FloatArray(16000) { i ->
            val t = i / 8000.0 - .2
            if (t < 0) 0f else (.3 * exp(-7*t) * (sin(2*PI*350*t) + sin(2*PI*700*t))).toFloat()
        }
        val old = com.tomtune.audio.lot0.TomStrikeAnalyzer()
        val metadata = mutableListOf<ImpactDiagnostic>()
        val instrumented = TomStrikeAnalyzer(diagnostic = { metadata.add(it) })
        val previous = WavPcm(samples, 8000).blocks(80).flatMap { old.accept(it).asSequence() }.toList()
        val actual = WavPcm(samples, 8000).blocks(80).flatMap { instrumented.accept(it).asSequence() }.toList()
        assertEquals(previous, actual)
        assertEquals("historical_harmonic_lower", metadata.single().rule)
        assertEquals(350.0, actual.filterIsInstance<StrikeEvent.Estimate>().single().frequencyHz, .5)
    }
    @Test fun resetAndDiscontinuityDoNotBridgeCaptures() {
        val lines = mutableListOf<String>()
        val comparison = LugDiagnosticComparison { lines.add(it) }
        blocks(8000, 1).take(25).forEach { comparison.accept(it) } // pending at interruption
        comparison.reset()
        blocks(8000, 1).forEach { comparison.accept(it) }
        assertEquals(1, lines.count { it.startsWith("CMP Impact ") })
        lines.clear()
        comparison.accept(PcmBlock(FloatArray(8000), 8000, 0))
        assertTrue(lines.any { "reason=pcm_discontinuity" in it })
        assertTrue(comparison.finish().isEmpty())
    }
}
