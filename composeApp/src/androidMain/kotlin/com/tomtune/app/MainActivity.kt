package com.tomtune.app

import com.tomtune.session.MeasurementController

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tomtune.audio.AndroidPcmSource
import com.tomtune.contracts.*
import com.tomtune.scene.FilamentTomView
import java.text.NumberFormat

class MainActivity : ComponentActivity() {
    private val traceRun = System.nanoTime()
    private lateinit var source: AndroidPcmSource
    private lateinit var controller: MeasurementController
    private var pendingSink: PcmSink? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val sink = pendingSink
        pendingSink = null
        if (sink != null) {
            if (granted) source.start(sink) else sink.onIssue(CaptureIssue.PermissionDenied)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val diagnostics = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        source = AndroidPcmSource(this) { rate, origin ->
            if (diagnostics) Log.i("TomTuneTrace", "CLOCK run=$traceRun session=${controller.state.value.generation} rate=$rate originNs=$origin tNs=${System.nanoTime()}")
        }
        controller = MeasurementController(stopCapture = { pendingSink = null; source.stop() }, onEvent = { session, event ->
            if (diagnostics) trace("ANALYSIS", session, event, System.nanoTime())
        })
        setContent { App(controller, startCapture = { sink ->
            if (diagnostics) Log.i("TomTuneTrace", "START run=$traceRun session=${controller.state.value.generation} tNs=${System.nanoTime()}")
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) source.start(sink)
            else { pendingSink = sink; permission.launch(Manifest.permission.RECORD_AUDIO) }
        }, onFrame = if (diagnostics) { state, time, lug ->
            state.event?.let { trace("FRAME", state.generation, it, time, lug) }
        } else null) }
    }
    private fun trace(phase: String, session: Long, event: StrikeEvent, time: Long, lug: Int = -1) {
        val kind = when (event) {
            is StrikeEvent.Detected -> "Detected"
            is StrikeEvent.Estimate -> "Estimate"
            is StrikeEvent.Rejected -> "Rejected"
        }
        val hz = (event as? StrikeEvent.Estimate)?.frequencyHz
        val reason = (event as? StrikeEvent.Rejected)?.reason
        Log.i("TomTuneTrace", "$phase run=$traceRun session=$session strike=${event.strikeId} kind=$kind pcmSeconds=${event.timeSeconds} tNs=$time hz=$hz reason=$reason lug=$lug")
    }
    override fun onStop() { controller.interrupt(); super.onStop() }
    override fun onDestroy() { controller.dispose(); super.onDestroy() }
}

@Composable
actual fun NativeTomScene(state: TomSceneState, onSelect: (Int) -> Unit, modifier: Modifier) {
    val callback by rememberUpdatedState(onSelect)
    AndroidView(factory = { context -> FilamentTomView(context) { callback(it) } },
        modifier = modifier, update = { it.update(state) }, onRelease = { it.close() })
}
actual fun formatHz(value: Double): String = NumberFormat.getNumberInstance().apply {
    maximumFractionDigits = 1; minimumFractionDigits = 1
}.format(value)
