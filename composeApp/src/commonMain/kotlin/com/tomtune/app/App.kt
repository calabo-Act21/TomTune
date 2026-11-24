package com.tomtune.app

import com.tomtune.session.MeasurementController
import com.tomtune.session.MeasurementState

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tomtune.contracts.*
import com.tomtune.app.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
expect fun NativeTomScene(state: TomSceneState, onSelect: (Int) -> Unit, modifier: Modifier)
expect fun formatHz(value: Double): String

@Composable
fun App(controller: MeasurementController, startCapture: (PcmSink) -> Unit,
        onFrame: ((MeasurementState, Long, Int) -> Unit)? = null) {
    val measurement by controller.state.collectAsState()
    var selected by remember { mutableStateOf(0) }
    var closed by remember { mutableStateOf(false) }
    fun selectLug(index: Int) {
        if (selected == index) return
        selected = index
        // A strike belongs to the selected lug/session; discard any former result or pending PCM.
        if (measurement.listening) controller.start(startCapture) else controller.stop()
    }
    LaunchedEffect(measurement.generation, measurement.event) {
        if (measurement.event != null && onFrame != null) {
            withFrameNanos { frameTime -> onFrame(measurement, frameTime, selected) }
        }
    }
    DisposableEffect(controller) { onDispose { controller.dispose() } }
    LaunchedEffect(measurement.issue) { if (measurement.issue != null) controller.releaseAfterIssue() }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xff1269d3), background = Color(0xffede9e3))) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeContentPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TomTune", style = MaterialTheme.typography.titleLarge)
            if (closed) {
                Text(stringResource(Res.string.closed))
                Button(onClick = { closed = false }) { Text(stringResource(Res.string.reopen)) }
            } else {
                Text(stringResource(Res.string.prototype), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(Res.string.head), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.target, formatHz(185.0)))
                NativeTomScene(TomSceneState(8, selected), { index -> selectLug(index) },
                    Modifier.fillMaxWidth().height(250.dp))
                Text(stringResource(Res.string.selected, selected + 1), style = MaterialTheme.typography.labelLarge)
                // Same controls as native picking, with 48dp minimum touch targets and localized labels.
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (index in 0..7) {
                        FilterChip(selected = selected == index, onClick = { selectLug(index) },
                            label = { Text(stringResource(Res.string.lug, index + 1)) },
                            modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                Text(stringResource(Res.string.instruction), style = MaterialTheme.typography.bodySmall)
                Card(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val event = measurement.event
                        val issue = measurement.issue
                        Text(stringResource(when (issue) {
                            CaptureIssue.PermissionDenied -> Res.string.denied
                            CaptureIssue.Interrupted -> Res.string.interrupted
                            CaptureIssue.RouteChanged -> Res.string.route
                            CaptureIssue.Unavailable -> Res.string.unavailable
                            CaptureIssue.Overrun -> Res.string.overrun
                            null -> if (event != null) Res.string.detected else if (measurement.listening) Res.string.waiting else Res.string.stopped
                        }))
                        when (event) {
                            is StrikeEvent.Estimate -> {
                                Text("${formatHz(event.frequencyHz)} Hz", style = MaterialTheme.typography.displaySmall)
                                Text(stringResource(Res.string.provisional))
                            }
                            is StrikeEvent.Rejected -> Text(stringResource(Res.string.rejected))
                            else -> Unit
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (measurement.listening) controller.stop() else controller.start(startCapture) },
                        modifier = Modifier.weight(1f)) {
                        Text(stringResource(if (measurement.listening) Res.string.stop else Res.string.enable))
                    }
                    TextButton(onClick = { controller.stop(); closed = true }) { Text(stringResource(Res.string.close)) }
                }
                Text(stringResource(Res.string.reference), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
