package com.tomtune.audio

import com.tomtune.contracts.StrikeEvent

/** Metadata only. Power is FFT-bin power (not calibrated SPL); dB uses power ratios. */
data class OnsetDiagnostic(val strikeId: Long, val frame: Long, val rate: Int,
    val rms: Double, val peak: Double, val noise: Double, val envelope: Double)
data class DiagnosticPeak(val hz: Double, val power: Double, val relativeDb: Double, val selectionEligible: Boolean)
data class DiagnosticWindow(val startSeconds: Double, val concentration: Double, val peaks: List<DiagnosticPeak>)
data class DiagnosticTrack(val hz: Double, val frequencies: List<Double?>, val powers: List<Double?>,
    val decayDb: List<Double?>, val eligible: Boolean)
data class ImpactDiagnostic(val strikeId: Long, val frame: Long, val rate: Int,
    val attackRms: Double, val attackPeak: Double, val clippedFraction: Double,
    val windows: List<DiagnosticWindow>, val tracks: List<DiagnosticTrack>, val rule: String,
    val persistenceAdvantage: Double?, val result: StrikeEvent)

/** One short line per window/track, below Android's log entry limit. No PCM serialization. */
fun ImpactDiagnostic.traceLines(): List<String> = buildList {
    add("Attack rms20ms=$attackRms peak20ms=$attackPeak clippedFraction=$clippedFraction")
    windows.forEachIndexed { i, w ->
        add("Window index=$i offsetSeconds=${w.startSeconds} concentration=${w.concentration} peaks=" +
            w.peaks.joinToString(";") { "${it.hz},${it.power},${it.relativeDb},${it.selectionEligible}" })
    }
    tracks.forEachIndexed { i, t ->
        add("Mode index=$i hz=${t.hz} eligible=${t.eligible} frequencies=${t.frequencies.joinToString(",")} " +
            "powers=${t.powers.joinToString(",")} decayDb=${t.decayDb.joinToString(",")}")
    }
    add("Selection rule=$rule advantage=$persistenceAdvantage " + when (val e = result) {
        is StrikeEvent.Estimate -> "hz=${e.frequencyHz} confidence=${e.confidence}"
        is StrikeEvent.Rejected -> "reason=${e.reason}"
        else -> error("Expected terminal result")
    })
}
