package com.tomtune.audio

import com.tomtune.contracts.StrikeEvent
import java.io.File
import kotlin.math.abs
import kotlin.math.log2
import kotlin.test.*

class ReferenceWavTest {
    @Test fun eightAnnotatedImpactsUseTheStreamingAnalyzer() {
        val root = File(System.getProperty("tomtune.root", ".."))
        val expected = File(root, "docs/lot0/frappes.csv").readLines().drop(1).filter { it.isNotBlank() }.map { row -> row.split(',').map { it.toDouble() } }
        val wav = WavPcm.decode(File(root, "docs/lot0/frappes.wav").readBytes())
        val analyzer = TomStrikeAnalyzer()
        val events = wav.blocks(480).flatMap { analyzer.accept(it).asSequence() }.toList() + analyzer.finish()
        val hits = events.filterIsInstance<StrikeEvent.Detected>()
        val estimates = events.filterIsInstance<StrikeEvent.Estimate>()
        assertEquals(8, hits.size, events.toString())
        assertEquals(8, estimates.size, events.toString())
        assertEquals(8, estimates.map { it.strikeId }.distinct().size)
        expected.zip(hits.zip(estimates)).forEach { (reference, pair) ->
            val (hit, estimate) = pair
            val cents = 1200 * log2(estimate.frequencyHz / reference[1])
            assertTrue(abs(hit.timeSeconds - reference[0]) <= 0.010, "Onset: $hit vs $reference")
            assertTrue(abs(cents) <= 12, "Frequency: $estimate vs $reference; error=$cents cents")
            assertEquals(hit.strikeId, estimate.strikeId)
            assertTrue(estimate.timeSeconds > hit.timeSeconds)
            println("WAV impact=${reference[0]}s detected=${hit.timeSeconds}s result=${estimate.timeSeconds}s Hz=${estimate.frequencyHz} errorCents=$cents")
        }
    }
}
