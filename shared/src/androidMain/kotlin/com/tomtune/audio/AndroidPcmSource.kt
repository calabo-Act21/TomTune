package com.tomtune.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import com.tomtune.contracts.*
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong

/** AudioRecord reads on a dedicated audio worker. PCM exists only in memory. */
class AndroidPcmSource(context: Context) : PcmSource {
    private val app = context.applicationContext
    private val lock = Any()
    private val generation = AtomicLong()
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private var routingThread: HandlerThread? = null
    private var recordingCallback: AudioManager.AudioRecordingCallback? = null
    private var routingListener: AudioRouting.OnRoutingChangedListener? = null

    override fun start(sink: PcmSink) {
        stop()
        val token = generation.incrementAndGet()
        worker = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            if (app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                issue(token, sink, CaptureIssue.PermissionDenied); return@Thread
            }
            var audio: AudioRecord? = null
            try {
                val requestedRate = 48000
                val minimum = AudioRecord.getMinBufferSize(requestedRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                if (minimum <= 0) { issue(token, sink, CaptureIssue.Unavailable); return@Thread }
                val supportsRaw = app.getSystemService(AudioManager::class.java).getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
                val source = if (supportsRaw) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
                val active = AudioRecord.Builder().setAudioSource(source)
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(requestedRate).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
                    .setBufferSizeInBytes(maxOf(minimum * 4, 19200)).build()
                audio = active
                if (active.state != AudioRecord.STATE_INITIALIZED) { issue(token, sink, CaptureIssue.Unavailable); return@Thread }
                synchronized(lock) {
                    if (generation.get() != token) { active.release(); audio = null; return@Thread }
                    recorder = active
                active.startRecording()
                var initialDevice = active.routedDevice?.id
                val routing = HandlerThread("TomTune-route").also { it.start() }
                synchronized(lock) { routingThread = routing }
                val listener = AudioRouting.OnRoutingChangedListener { route ->
                    val device = route.routedDevice?.id
                    if (initialDevice == null) initialDevice = device
                    else if (device != initialDevice) issue(token, sink, CaptureIssue.RouteChanged)
                }
                routingListener = listener
                active.addOnRoutingChangedListener(listener, Handler(routing.looper))
                if (Build.VERSION.SDK_INT >= 29) {
                    val callback = object : AudioManager.AudioRecordingCallback() {
                        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
                            if (configs.any { it.clientAudioSessionId == active.audioSessionId && it.isClientSilenced }) issue(token, sink, CaptureIssue.Interrupted)
                        }
                    }
                    recordingCallback = callback
                    active.registerAudioRecordingCallback(Executor { task -> Handler(routing.looper).post(task) }, callback)
                }
                }
                val buffer = ShortArray(480)
                var position = 0L
                while (generation.get() == token) {
                    val count = active.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (count <= 0) { issue(token, sink, CaptureIssue.Interrupted); break }
                    val copy = FloatArray(count) { buffer[it] / 32768f }
                    synchronized(lock) {
                        if (generation.get() == token) sink.onPcm(PcmBlock(copy, active.sampleRate, position))
                    }
                    position += count
                }
            } catch (_: SecurityException) { issue(token, sink, CaptureIssue.PermissionDenied) }
            catch (_: Exception) { issue(token, sink, CaptureIssue.Unavailable) }
            finally {
                synchronized(lock) {
                    if (recorder === audio) {
                        cleanup(audio)
                        recorder = null
                    } else audio?.let { old -> runCatching { old.release() } }
                }
            }
        }, "TomTune-PCM").also { it.start() }
    }

    private fun issue(token: Long, sink: PcmSink, issue: CaptureIssue) {
        synchronized(lock) {
            if (generation.get() != token) return
            generation.incrementAndGet()
            sink.onIssue(issue)
        }
        // Close outside callback or capture loop to avoid waiting on its own dispatcher.
        Thread({
            synchronized(lock) {
                if (generation.get() == token + 1) { cleanup(recorder); recorder = null }
            }
        }, "TomTune-PCM-close").start()
    }
    private fun cleanup(audio: AudioRecord?) {
        if (audio != null) {
            routingListener?.let { runCatching { audio.removeOnRoutingChangedListener(it) } }
            if (Build.VERSION.SDK_INT >= 29) recordingCallback?.let { runCatching { audio.unregisterAudioRecordingCallback(it) } }
            runCatching { audio.stop() }; runCatching { audio.release() }
        }
        routingThread?.quitSafely(); routingThread = null
        routingListener = null; recordingCallback = null
    }
    override fun stop() {
        synchronized(lock) {
            generation.incrementAndGet()
            cleanup(recorder); recorder = null
            worker = null
        }
    }
}
