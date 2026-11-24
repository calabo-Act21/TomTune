package com.tomtune.app

import com.tomtune.contracts.*
import kotlinx.coroutines.*
import kotlin.math.*

/** Explicit debug-intent fixture only. No playback, files, network or microphone.
 * The host visibly labels injection; the normal launch always uses AndroidPcmSource. */
internal class DiagnosticPcmSource(private val scope: CoroutineScope, private val frequency: () -> Double) : PcmSource {
    private val lock = Any()
    private var generation = 0L
    private var job: Job? = null
    override fun start(sink: PcmSink) = synchronized(lock) {
        stop()
        val token = ++generation
        job = scope.launch(Dispatchers.Default) {
            var frame = 0L
            while (isActive) {
                val hz = frequency()
                repeat(100) { block ->
                    val samples = FloatArray(80) { index ->
                        val t = (block * 80 + index) / 8000.0 - .10
                        if (t < 0) 0f else (.35 * exp(-9 * t) * sin(2 * PI * hz * t)).toFloat()
                    }
                    synchronized(lock) { if (token == generation) sink.onPcm(PcmBlock(samples, 8000, frame)) }; frame += 80
                    delay(10)
                }
            }
        }
    }
    override fun stop() = synchronized(lock) { generation++; job?.cancel(); job = null }
}
