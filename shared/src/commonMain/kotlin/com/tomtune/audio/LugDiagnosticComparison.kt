package com.tomtune.audio

import com.tomtune.contracts.*
import com.tomtune.tuning.lugAnalysisParameters
import kotlin.math.abs
import kotlin.math.roundToInt

/** Three native detectors + analysis on a shared capture at the earliest union onset.
 * No target, no mode lock, no change to the current analyzer's product events.
 * The ring is trimmed to 400 ms after each block; no PCM leaves this object.
 */
class LugDiagnosticComparison(private val trace: (String) -> Unit) : StrikeAnalyzer {
    private data class Impact(val id: Long, var frame: Long, val native: MutableMap<String, Long> = mutableMapOf())
    private val configurations = linkedMapOf(
        "lot0" to AnalysisParameters(),
        "current" to lugAnalysisParameters(),
        "band80_700" to lugAnalysisParameters().copy(minHz = 80.0, maxHz = 700.0),
    )
    private var ordinal = 0L
    private var rate = 0
    private var nextFrame: Long? = null
    private var bufferStart = 0L
    private val buffer = ArrayDeque<Float>()
    private val impacts = mutableListOf<Impact>()
    private val nativeIds = mutableMapOf<Pair<String, Long>, Long>()
    private val analyzers = configurations.mapValues { (name, params) ->
        TomStrikeAnalyzer(params, diagnostic = { report ->
            val impactId = nativeIds.remove(name to report.strikeId)
            report.traceLines().forEach { detail ->
                trace("CMP phase=native config=$name impact=$impactId nativeStrike=${report.strikeId} frame=${report.frame} rate=${report.rate} $detail")
            }
        }, onset = { onset ->
            val impact = impacts.firstOrNull { abs(it.frame - onset.frame) <= onset.rate * .040 }
                ?: Impact(++ordinal, onset.frame).also { impacts.add(it) }
            impact.frame = minOf(impact.frame, onset.frame)
            impact.native[name] = onset.frame
            nativeIds[name to onset.strikeId] = impact.id
            trace("CMP NativeDetected config=$name impact=${impact.id} nativeStrike=${onset.strikeId} frame=${onset.frame} rate=${onset.rate} rms5ms=${onset.rms} peak5ms=${onset.peak} noise=${onset.noise} envelope=${onset.envelope}")
        })
    }
    override fun accept(block: PcmBlock): List<StrikeEvent> {
        if (nextFrame != null && (nextFrame != block.startFrame || rate != block.sampleRate)) {
            trace("CMP Reset reason=pcm_discontinuity"); reset()
        }
        if (nextFrame == null) { bufferStart = block.startFrame; rate = block.sampleRate }
        buffer.addAll(block.samples.toList())
        nextFrame = block.startFrame + block.samples.size
        val results = analyzers.mapValues { (_, analyzer) -> analyzer.accept(block) }
        completeImpacts()
        val keepFrom = nextFrame!! - (rate * .40).roundToInt()
        while (bufferStart < keepFrom && buffer.isNotEmpty()) { buffer.removeFirst(); bufferStart++ }
        return results.getValue("current")
    }
    private fun completeImpacts() {
        val count = (rate * .32).toInt()
        val completed = impacts.filter { it.frame + count <= nextFrame!! }
        for (impact in completed) {
            val offset = (impact.frame - bufferStart).toInt()
            if (offset < 0 || offset + count > buffer.size) {
                trace("CMP Incomplete impact=${impact.id} frame=${impact.frame} reason=buffer_gap")
                continue
            }
            val samples = (offset until offset + count).map { buffer[it] }
            trace("CMP Impact impact=${impact.id} frame=${impact.frame} rate=$rate pcmSeconds=${impact.frame.toDouble()/rate} " +
                configurations.keys.joinToString(" ") { "$it=${impact.native[it] ?: "missed"}" })
            configurations.forEach { (name, params) ->
                TomStrikeAnalyzer(params, diagnostic = { report ->
                    report.traceLines().forEach { detail ->
                        trace("CMP phase=paired config=$name impact=${impact.id} frame=${impact.frame} rate=$rate $detail")
                    }
                }).evaluateImpact(samples, rate, impact.frame, impact.id)
            }
        }
        impacts.removeAll(completed.toSet())
    }
    override fun finish(): List<StrikeEvent> {
        val events = analyzers.mapValues { (_, a) -> a.finish() }.getValue("current")
        impacts.forEach { trace("CMP Incomplete impact=${it.id} frame=${it.frame} reason=end_before_320ms") }
        impacts.clear(); nativeIds.clear(); buffer.clear()
        return events
    }
    override fun reset() {
        analyzers.values.forEach { it.reset() }
        impacts.clear(); nativeIds.clear(); buffer.clear(); bufferStart = 0; nextFrame = null; rate = 0; ordinal = 0
    }
}
