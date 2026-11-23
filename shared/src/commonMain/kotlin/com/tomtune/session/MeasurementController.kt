package com.tomtune.session

import com.tomtune.audio.TomStrikeAnalyzer
import com.tomtune.contracts.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MeasurementState(val listening: Boolean = false, val event: StrikeEvent? = null,
                            val issue: CaptureIssue? = null, val generation: Long = 0)
/** One ordered analysis worker; no PCM is processed on the rendering thread. */
class MeasurementController(
    private val stopCapture: () -> Unit,
    analysisDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val onEvent: (Long, StrikeEvent) -> Unit = { _, _ -> },
) {
    private data class Work(val generation: Long, val block: PcmBlock)
    private val mutableState = MutableStateFlow(MeasurementState())
    val state: StateFlow<MeasurementState> = mutableState
    private val queue = Channel<Work>(32)
    private val scope = CoroutineScope(SupervisorJob() + analysisDispatcher)
    init {
        scope.launch {
            val analyzer = TomStrikeAnalyzer()
            var current = -1L
            for (work in queue) {
                if (!isActiveSession(work.generation)) continue
                if (current != work.generation) { analyzer.reset(); current = work.generation }
                analyzer.accept(work.block).forEach { event ->
                    if (publish(work.generation) { it.copy(event = event) }) {
                        runCatching { onEvent(work.generation, event) }
                    }
                }
            }
        }
    }
    private fun isActiveSession(id: Long) = mutableState.value.let { it.generation == id && it.listening }
    private fun publish(id: Long, change: (MeasurementState) -> MeasurementState): Boolean {
        while (true) {
            val old = mutableState.value
            if (old.generation != id || !old.listening) return false
            if (mutableState.compareAndSet(old, change(old))) return true
        }
    }
    /** Called on the host UI thread, as are stop/dispose. */
    fun start(startCapture: (PcmSink) -> Unit) {
        stop()
        val id = mutableState.value.generation + 1
        mutableState.value = MeasurementState(listening = true, generation = id)
        startCapture(object : PcmSink {
            override fun onPcm(block: PcmBlock) {
                if (!isActiveSession(id)) return
                if (queue.trySend(Work(id, block)).isFailure) onIssue(CaptureIssue.Overrun)
            }
            override fun onIssue(issue: CaptureIssue) {
                publish(id) { MeasurementState(issue = issue, generation = id + 1) }
                // Host observes the fault and releases capture on its UI thread.
            }
        })
    }
    fun stop() {
        mutableState.value = MeasurementState(generation = mutableState.value.generation + 1)
        stopCapture()
    }
    fun interrupt() {
        val old = mutableState.value
        if (old.listening) {
            mutableState.value = MeasurementState(issue = CaptureIssue.Interrupted, generation = old.generation + 1)
        }
        stopCapture()
    }
    fun releaseAfterIssue() { stopCapture() }
    fun dispose() { stop(); queue.close(); scope.cancel() }
}
