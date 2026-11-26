package com.tomtune.app

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import com.tomtune.audio.AndroidPcmSource
import com.tomtune.contracts.*
import com.tomtune.tuning.TuningController
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tomtune.kit.*
import kotlinx.coroutines.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var model: KitModel
    private var permissionToken: Long? = null
    private var debugReceiver: android.content.BroadcastReceiver? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val token = permissionToken
        permissionToken = null
        if (token != null) model.source.permissionResult(token, granted)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent))
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = KitModel(applicationContext,
                applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0 && intent.getBooleanExtra("tomtune.injectedPcm", false), intent.getBooleanExtra("tomtune.lugProbe", false), intent.getBooleanExtra("tomtune.modeProbe", false)) as T
        })[KitModel::class.java]
        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                    val command = intent.getStringExtra("command") ?: return
                    if (command == "reset") model.tuning.debugResetSeries()
                    if (command in listOf("reset", "cue", "begin", "end", "observed")) {
                        // Bound/allow-list tokens; never accept file paths, PCM or arbitrary log payloads.
                        val block = intent.getStringExtra("block")?.takeIf { it in listOf("light", "firm", "injected") } ?: "unknown"
                        val ordinal = intent.getIntExtra("ordinal", 0).coerceIn(0, 10)
                        model.tuning.debugMarker("command=$command block=$block ordinal=$ordinal monotonicNanos=${System.nanoTime()} epochMillis=${System.currentTimeMillis()}")
                    }
                }
            }
            debugReceiver = receiver
            val filter = android.content.IntentFilter("com.tomtune.app.DEBUG_LUG")
            if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, android.content.Context.RECEIVER_EXPORTED)
            else registerReceiver(receiver, filter)
        }
        model.source.request = { token ->
            if (permissionToken == null) { permissionToken = token; permission.launch(Manifest.permission.RECORD_AUDIO) }
            else model.source.permissionResult(token, false)
        }
        setContent {
            val state by model.controller.state.collectAsState()
            val tuning by model.tuning.state.collectAsState()
            BackHandler(enabled = tuning != null || state.screen in listOf(KitScreen.CONFIGURE, KitScreen.PREVIEW, KitScreen.DRUM) || state.busy) {
                if (tuning != null) model.tuning.requestAbandon() else model.controller.back()
            }
            KitApp(model.controller, model.catalogue,
                kitVisual = { kit, modifier, onOpen -> AndroidKitVisual(kit, model.catalogue, modifier, onOpen) },
                focusVisual = { drum, modifier -> AndroidFocusVisual(drum, model.catalogue, modifier) }, tuning = model.tuning, injectedPcm = model.injectedPcm, tuningVisual = { s, select -> AndroidTuningVisual(s, select) })
        }
    }
    override fun onStop() { model.tuning.interrupt(); super.onStop() }
    override fun onDestroy() { debugReceiver?.let { unregisterReceiver(it) }; debugReceiver = null; model.source.request = null; super.onDestroy() }
}

/** A permission reply can only release the sink for the exact request that opened it. */
private class PermissionPcmSource(private val context: android.content.Context) : PcmSource {
    var onClock: ((Int, Long) -> Unit)? = null
    private val microphone = AndroidPcmSource(context) { rate, origin -> onClock?.invoke(rate, origin) }
    var request: ((Long) -> Unit)? = null
    private var serial = 0L
    private var pending: Pair<Long, PcmSink>? = null
    override fun start(sink: PcmSink) {
        stop()
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) microphone.start(sink)
        else {
            val id = ++serial
            pending = id to sink
            request?.invoke(id) ?: permissionResult(id, false)
        }
    }
    fun permissionResult(id: Long, granted: Boolean) {
        val saved = pending?.takeIf { it.first == id } ?: return
        pending = null
        if (granted) microphone.start(saved.second) else saved.second.onIssue(CaptureIssue.PermissionDenied)
    }
    override fun stop() { serial++; pending = null; microphone.stop() }
}

private class KitModel(context: android.content.Context, val injectedPcm: Boolean, lugProbe: Boolean, modeProbe: Boolean) : ViewModel() {
    private val traceRun = System.nanoTime()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val catalogue = readVisualCatalogue(context)
    val controller = KitController(AndroidKitRepository(context), KitRules({ UUID.randomUUID().toString() }, catalogue), scope)
    val source = PermissionPcmSource(context)
    val tuning: TuningController
    init {
        val input = if (injectedPcm) DiagnosticPcmSource(scope, { diagnosticFrequency() }, lugProbe, modeProbe) else source
        tuning = TuningController(input, scope, { session, reference ->
            controller.finishTuning(session, reference, System.currentTimeMillis())
        }, diagnosticComparison = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0,
            trace = { detail ->
            if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
                android.util.Log.i("TomTuneTrace", "LUG run=$traceRun input=${if (injectedPcm) "injected" else "microphone"} $detail")
        })
        source.onClock = { rate, origin ->
            tuning.debugMarker("CLOCK rate=$rate originMonotonicNanos=$origin monotonicNanos=${System.nanoTime()} epochMillis=${System.currentTimeMillis()}")
        }
    }
    private fun diagnosticFrequency(): Double {
        val s = tuning.state.value ?: return 185.0
        return if (s.stage == com.tomtune.tuning.TuningStage.CENTER) 120.0 else s.localTarget
    }
    override fun onCleared() { tuning.dispose(); scope.cancel() }
}
