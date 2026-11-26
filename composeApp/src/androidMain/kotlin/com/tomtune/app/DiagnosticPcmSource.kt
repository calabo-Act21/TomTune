package com.tomtune.app

import com.tomtune.contracts.*
import com.tomtune.audio.ModeComparisonFixture
import kotlinx.coroutines.*
import kotlin.math.*

/** Explicit debug-intent fixture only. No playback, files, network or microphone.
 * The host visibly labels injection; the normal launch always uses AndroidPcmSource. */
internal class DiagnosticPcmSource(private val scope: CoroutineScope, private val frequency: () -> Double,
    private val lugProbe: Boolean = false, private val modeProbe: Boolean = false) : PcmSource {
    private val lock = Any()
    private var generation = 0L
    private var job: Job? = null
    override fun start(sink: PcmSink) = synchronized(lock) {
        stop()
        val token = ++generation
        job = scope.launch(Dispatchers.Default) {
            var frame = 0L
            var strike = 0
            if (modeProbe) {
                while (isActive) {
                    val shape = ModeComparisonFixture.stroke(strike)
                    android.util.Log.i("TomTuneTrace", "FIXTURE input=injected block=${if (strike % 20 < 10) "low" else "high"} ordinal=${strike % 10 + 1} frame=${frame + 9600} rate=48000 amplitudes=${shape.amplitudes.joinToString(",")} decays=${shape.decays.joinToString(",")}")
                    repeat(200) { block ->
                        val samples = ModeComparisonFixture.block(strike, block * 480, 480, 48000)
                        synchronized(lock) { if (token == generation) sink.onPcm(PcmBlock(samples, 48000, frame)) }
                        frame += 480
                        delay(10)
                    }
                    strike++
                }
                return@launch
            }
            while (isActive) {
                val hz = if (lugProbe) { if (strike <= 10) 350.0 else 700.0 } else frequency()
                val amplitude = if (lugProbe && strike < 10) .025 else .35
                repeat(if (lugProbe) 200 else 100) { block ->
                    val samples = FloatArray(80) { index ->
                        val t = (block * 80 + index) / 8000.0 - .10
                        if (t < 0) 0f else {
                            val primary = amplitude * exp(-7 * t) * sin(2 * PI * hz * t)
                            val second = if (lugProbe && strike == 10) .35 * exp(-7 * t) * sin(2 * PI * 700 * t) else 0.0
                            (primary + second).toFloat()
                        }
                    }
                    synchronized(lock) { if (token == generation) sink.onPcm(PcmBlock(samples, 8000, frame)) }; frame += 80
                    delay(10)
                }
                strike++
            }
        }
    }
    override fun stop() = synchronized(lock) { generation++; job?.cancel(); job = null }
}
