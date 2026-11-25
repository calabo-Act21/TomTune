package com.tomtune.tuning

import com.tomtune.audio.AnalysisParameters
import com.tomtune.contracts.*
import com.tomtune.kit.*
import com.tomtune.session.MeasurementController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

fun cents(hz: Double, target: Double) = 1200 * log2(hz / target)
fun midiHz(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)
fun nearestMidi(hz: Double) = (69 + 12 * log2(hz / 440)).roundToInt()
fun suggestedHz(diameter: Int) = midiHz(mapOf(8 to 53, 10 to 50, 12 to 47, 13 to 45,
    14 to 43, 15 to 40, 16 to 38, 18 to 36).getValue(diameter))

/** Versioned debug hypothesis. Independent of signal quality and series dispersion. */
data class TuningProfile(val version: Int = 1, val batter: Double = 1.50, val resonant: Double = 1.85,
    val localToleranceCents: Double = 12.0, val globalToleranceCents: Double = 20.0)
/** Local sensitivity hypothesis; absolute threshold lowered only for the near-lug gesture. */
fun lugAnalysisParameters() = AnalysisParameters(minHz = 55.0, maxHz = 900.0, onsetRms = 0.010,
    requireDecayEvidenceForOctaves = true)

enum class Head { BATTER, RESONANT }
enum class TuningStage { TARGET, LUGS, ADVICE, CENTER }
enum class Feedback { WAITING, DETECTED, PROVISIONAL, REJECTED, UNSTABLE, CONFIRMED }
fun schematicLugAnchors(count: Int): List<NormalPoint> {
    require(count in 3..12)
    return (0 until count).map { index ->
        val angle = 2 * PI * (index + .5) / count - PI / 2
        NormalPoint(.5f + .35f * cos(angle).toFloat(), .5f + .35f * sin(angle).toFloat())
    }
}
enum class Advice { TIGHTEN, LOOSEN, LEAVE }
fun advice(hz: Double, target: Double, tolerance: Double = 12.0) = when {
    cents(hz, target) < -tolerance -> Advice.TIGHTEN
    cents(hz, target) > tolerance -> Advice.LOOSEN
    else -> Advice.LEAVE
}

class StrikeSeries {
    private val values = mutableListOf<Double>()
    var unstable = false; private set
    val count get() = values.size
    fun reset() { values.clear(); unstable = false }
    fun add(hz: Double): Double? {
        require(hz.isFinite() && hz > 0)
        values.add(hz)
        if (values.size > 5) values.removeAt(0)
        val spread = cents(values.max(), values.min())
        unstable = spread > 18.0 + 1e-8
        if (unstable) { values.clear(); values.add(hz); return null }
        if ((values.size >= 3 && spread <= 8.0 + 1e-8) || values.size >= 5) {
            val sorted = values.sorted()
            return sorted[sorted.size / 2]
        }
        return null
    }
}

data class TuningState(
    val sessionId: Long, val kitId: String, val drum: Drum,
    val targetHz: Double, val targetOrigin: TargetOrigin, val fromReference: Boolean,
    val profile: TuningProfile = TuningProfile(), val stage: TuningStage = TuningStage.TARGET,
    val head: Head = Head.BATTER, val pass: Int = 1, val lug: Int = 0,
    val measurements: Map<Int, Double> = emptyMap(),
    val lastConfirmed: Pair<Int, Double>? = null, val centerHz: Double? = null,
    val feedback: Feedback = Feedback.WAITING, val provisionalHz: Double? = null,
    val seriesCount: Int = 0, val rejection: RejectionReason? = null,
    val stableHz: Double? = null, val readyForNext: Boolean = false,
    val listening: Boolean = false, val issue: CaptureIssue? = null,
    val abandonDialog: Boolean = false, val saving: Boolean = false, val writeFailed: Boolean = false,
) {
    val localTarget get() = targetHz * if (head == Head.BATTER) profile.batter else profile.resonant
    val needsCorrection get() = centerHz?.let { abs(cents(it, targetHz)) > profile.globalToleranceCents } == true
}

/** All domain mutations run on the host UI scope. Ordered analyzer callbacks are delivered
 * upstream of StateFlow, so conflated display states cannot drop estimates. Source is injectable. */
class TuningController(
    private val source: PcmSource, private val scope: CoroutineScope,
    private val save: suspend (TuningState, Boolean) -> Boolean,
    analysisDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val trace: (String) -> Unit = {},
) {
    private val mutable = MutableStateFlow<TuningState?>(null)
    val state = mutable.asStateFlow()
    private var serial = 0L
    private var generation = -1L
    internal val captureGeneration get() = generation
    private val series = StrikeSeries()
    private val pending = mutableSetOf<Long>()
    private var lastDetected = Long.MIN_VALUE
    private val measurement = MeasurementController(source::stop, analysisDispatcher) { id, event ->
        scope.launch { acceptEvent(id, event) }
    }
    init {
        scope.launch {
            measurement.state.collect { m ->
                if (m.issue != null && mutable.value?.listening == true && m.generation == generation + 1) {
                    measurement.releaseAfterIssue()
                    resetSeries()
                    mutable.value = mutable.value?.copy(listening = false, issue = m.issue,
                        feedback = Feedback.WAITING, provisionalHz = null, seriesCount = 0, readyForNext = false)
                }
            }
        }
    }
    fun open(kit: Kit, drum: Drum) {
        if (mutable.value?.saving == true) return
        require(drum.family.isTom && drum.lugs in 3..12)
        stop()
        mutable.value = TuningState(++serial, kit.id, drum, drum.reference?.hz ?: suggestedHz(drum.diameter),
            drum.reference?.origin ?: TargetOrigin.SUGGESTED, drum.reference != null)
    }
    fun chooseMidi(midi: Int) {
        val s = mutable.value ?: return
        if (s.stage == TuningStage.TARGET && midi in 28..60)
            mutable.value = s.copy(targetHz = midiHz(midi), targetOrigin = TargetOrigin.MANUAL, fromReference = false)
    }
    fun start() {
        val s = mutable.value ?: return
        if (s.stage != TuningStage.TARGET) return
        mutable.value = s.copy(stage = TuningStage.LUGS)
        arm()
    }
    private fun resetSeries() {
        if (series.count > 0) log("SeriesReset reason=context_or_capture count=${series.count}")
        series.reset(); pending.clear(); lastDetected = Long.MIN_VALUE
    }
    private fun log(detail: String) {
        val s = mutable.value ?: return
        trace("session=${s.sessionId} generation=$generation head=${s.head} pass=${s.pass} lug=${s.lug + 1} $detail")
    }
    private fun stop() { generation = -1; measurement.stop(); resetSeries() }
    private fun arm() {
        val s = mutable.value ?: return
        if (s.saving || s.abandonDialog || s.stage !in listOf(TuningStage.LUGS, TuningStage.CENTER) || s.centerHz != null) return
        resetSeries()
        mutable.value = s.copy(listening = true, issue = null, feedback = Feedback.WAITING,
            provisionalHz = null, seriesCount = 0, rejection = null, readyForNext = false)
        val params = if (s.stage == TuningStage.CENTER) AnalysisParameters(minHz = 35.0, maxHz = 550.0)
            else lugAnalysisParameters()
        generation = measurement.start(params, source::start)
        log("CaptureStart minHz=${params.minHz} maxHz=${params.maxHz} onsetRms=${params.onsetRms}")
    }
    /** Generation belongs to the capture started for this exact selected step. */
    internal fun acceptEvent(id: Long, event: StrikeEvent) {
        val s = mutable.value ?: return
        if (id != generation || !s.listening || !measurement.state.value.listening) return
        when (event) {
            is StrikeEvent.Detected -> {
                if (event.strikeId <= lastDetected) return
                lastDetected = event.strikeId
                pending.add(event.strikeId)
                log("Detected strike=${event.strikeId} pcmSeconds=${event.timeSeconds}")
                mutable.value = s.copy(feedback = Feedback.DETECTED, rejection = null)
            }
            is StrikeEvent.Rejected -> {
                if (!pending.remove(event.strikeId)) return
                log("Rejected strike=${event.strikeId} pcmSeconds=${event.timeSeconds} reason=${event.reason} count=${series.count}")
                mutable.value = s.copy(feedback = Feedback.REJECTED, rejection = event.reason, provisionalHz = null)
            }
            is StrikeEvent.Estimate -> {
                if (!pending.remove(event.strikeId) || !event.frequencyHz.isFinite() || event.frequencyHz <= 0) return
                log("Estimate strike=${event.strikeId} pcmSeconds=${event.timeSeconds} hz=${event.frequencyHz} confidence=${event.confidence}")
                val previousCount = series.count
                val confirmed = series.add(event.frequencyHz)
                if (series.unstable) log("SeriesReset reason=incompatible previousCount=$previousCount hz=${event.frequencyHz}")
                if (confirmed == null) {
                    mutable.value = s.copy(feedback = if (series.unstable) Feedback.UNSTABLE else Feedback.PROVISIONAL,
                        provisionalHz = event.frequencyHz, seriesCount = series.count, rejection = null, readyForNext = false)
                    return
                }
                val countInSeries = series.count
                log("Confirmed strike=${event.strikeId} hz=$confirmed count=$countInSeries")
                if (s.stage == TuningStage.CENTER) {
                    stop()
                    mutable.value = s.copy(centerHz = confirmed, listening = false, feedback = Feedback.CONFIRMED,
                        provisionalHz = null, seriesCount = countInSeries)
                } else {
                    // Confirmation is a live draft for this lug. Only Next commits and advances.
                    mutable.value = s.copy(stableHz = confirmed, readyForNext = true,
                        feedback = Feedback.CONFIRMED, provisionalHz = event.frequencyHz,
                        seriesCount = countInSeries, rejection = null)
                }
            }
        }
    }
    fun nextLug() {
        val s = mutable.value ?: return
        val confirmed = s.stableHz ?: return
        if (s.stage != TuningStage.LUGS || !s.readyForNext || !s.listening || s.abandonDialog) return
        log("Next hz=$confirmed")
        stop() // invalidate pending callbacks before changing the step
        val results = s.measurements + (s.lug to confirmed)
        val next = s.lug + 1
        mutable.value = s.copy(measurements = results, lastConfirmed = s.lug to confirmed,
            stableHz = null, readyForNext = false, listening = false,
            feedback = Feedback.WAITING, provisionalHz = null, seriesCount = 0,
            stage = if (results.size == s.drum.lugs) TuningStage.ADVICE else TuningStage.LUGS,
            lug = next % s.drum.lugs!!)
        if (mutable.value?.stage == TuningStage.LUGS) arm()
    }
    fun selectLug(index: Int) {
        val s = mutable.value ?: return
        if (s.saving || s.abandonDialog || s.stage !in listOf(TuningStage.LUGS, TuningStage.ADVICE) || index !in 0 until s.drum.lugs!!) return
        stop()
        mutable.value = s.copy(stage = TuningStage.LUGS, lug = index, listening = false,
            stableHz = s.measurements[index], readyForNext = false)
        arm()
    }
    fun repeatHead() {
        val s = mutable.value ?: return
        if (s.stage != TuningStage.ADVICE) return
        stop()
        mutable.value = s.copy(stage = TuningStage.LUGS, pass = s.pass + 1, lug = 0,
            measurements = emptyMap(), lastConfirmed = null, stableHz = null, readyForNext = false, listening = false)
        arm()
    }
    fun nextHead() {
        val s = mutable.value ?: return
        if (s.stage != TuningStage.ADVICE) return
        stop()
        mutable.value = s.copy(stage = if (s.head == Head.BATTER) TuningStage.LUGS else TuningStage.CENTER,
            head = Head.RESONANT, pass = 1, lug = 0, measurements = emptyMap(), lastConfirmed = null, stableHz = null, readyForNext = false, listening = false)
        arm()
    }
    fun remeasureCenter() {
        val s = mutable.value ?: return
        if (s.stage != TuningStage.CENTER || s.saving) return
        stop(); mutable.value = s.copy(centerHz = null, listening = false); arm()
    }
    fun correct() {
        val s = mutable.value ?: return
        if (s.stage != TuningStage.CENTER || s.centerHz == null || s.saving) return
        stop()
        mutable.value = s.copy(stage = TuningStage.LUGS, head = Head.BATTER, pass = 1, lug = 0,
            measurements = emptyMap(), lastConfirmed = null, stableHz = null, readyForNext = false, centerHz = null, listening = false)
        arm()
    }
    fun interrupt() {
        val s = mutable.value ?: return
        if (!s.listening) return
        stop()
        mutable.value = s.copy(listening = false, issue = CaptureIssue.Interrupted, feedback = Feedback.WAITING,
            provisionalHz = null, seriesCount = 0, readyForNext = false)
    }
    fun resume() { if (mutable.value?.listening == false) arm() }
    fun requestAbandon() {
        val s = mutable.value ?: return
        if (s.saving) return
        interrupt()
        mutable.value = mutable.value?.copy(abandonDialog = true)
    }
    fun cancelAbandon() { mutable.value = mutable.value?.copy(abandonDialog = false) }
    fun abandon() { if (mutable.value?.saving == true) return; stop(); mutable.value = null }
    fun finish(withReference: Boolean) {
        val s = mutable.value ?: return
        if (s.stage != TuningStage.CENTER || s.centerHz == null || s.saving || s.abandonDialog) return
        stop(); mutable.value = s.copy(saving = true, listening = false, writeFailed = false)
        scope.launch {
            if (save(s, withReference)) mutable.value = null
            else mutable.value = s.copy(saving = false, listening = false, writeFailed = true)
        }
    }
    fun dispose() { measurement.dispose() }
}
