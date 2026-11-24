package com.tomtune.audio

import com.tomtune.contracts.*
import kotlinx.coroutines.*

/** Real-time paced injected PCM: same capture contract, no playback and no microphone. */
class WavPcmSource(private val bytes: ByteArray, private val scope: CoroutineScope) : PcmSource {
    private val gate = CaptureLock()
    private var generation = 0L
    private var job: Job? = null
    override fun start(sink: PcmSink) = gate.withLock {
        stop()
        val token = ++generation
        job = scope.launch(Dispatchers.Default) {
            try {
                val wav = WavPcm.decode(bytes)
                for (block in wav.blocks((wav.sampleRate * 0.01).toInt())) {
                    ensureActive()
                    gate.withLock { if (generation == token) sink.onPcm(block) }
                    delay(10)
                }
                // Padding finishes the last strike through the same live accept path.
                val offset = wav.samples.size.toLong()
                for (index in 0 until 40) {
                    ensureActive()
                    val count = (wav.sampleRate * 0.01).toInt()
                    gate.withLock {
                        if (generation == token) sink.onPcm(PcmBlock(FloatArray(count), wav.sampleRate, offset + index.toLong() * count))
                    }
                    delay(10)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { gate.withLock { if (generation == token) sink.onIssue(CaptureIssue.Unavailable) } }
        }
    }
    override fun stop() = gate.withLock { generation++; job?.cancel(); job = null }
}
