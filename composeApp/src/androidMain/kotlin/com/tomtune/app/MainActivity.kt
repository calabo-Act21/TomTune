package com.tomtune.app

import com.tomtune.session.MeasurementController

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
        source = AndroidPcmSource(this)
        controller = MeasurementController(stopCapture = { pendingSink = null; source.stop() })
        setContent { App(controller) { sink ->
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) source.start(sink)
            else { pendingSink = sink; permission.launch(Manifest.permission.RECORD_AUDIO) }
        } }
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
