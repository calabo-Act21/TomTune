package com.tomtune.app

import com.tomtune.session.MeasurementController

import androidx.compose.foundation.background
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
fun App(controller: MeasurementController, startCapture: (PcmSink) -> Unit) {
    val measurement by controller.state.collectAsState()
    var selected by remember { mutableStateOf(0) }
    var closed by remember { mutableStateOf(false) }
    DisposableEffect(controller) { onDispose { controller.dispose() } }
    LaunchedEffect(measurement.issue) { if (measurement.issue != null) controller.releaseAfterIssue() }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xff1269d3), background = Color(0xffede9e3))) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeContentPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TomTune", style = MaterialTheme.typography.headlineMedium)
            if (closed) {
                Text(stringResource(Res.string.closed))
                Button(onClick = { closed = false }) { Text(stringResource(Res.string.reopen)) }
            } else {
                Text(stringResource(Res.string.prototype), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(Res.string.head), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.target, formatHz(185.0)))
                NativeTomScene(TomSceneState(8, selected), { index -> selected = index },
                    Modifier.fillMaxWidth().height(330.dp))
                Text(stringResource(Res.string.selected, selected + 1))
                // Same controls as native picking, with 48dp minimum touch targets and localized labels.
                for (row in 0..1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (column in 0..3) {
                        val index = row * 4 + column
                        FilterChip(selected = selected == index, onClick = { selected = index },
                            label = { Text(stringResource(Res.string.lug, index + 1)) },
                            modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
                Text(stringResource(Res.string.instruction))
                Card(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Button(onClick = { if (measurement.listening) controller.stop() else controller.start(startCapture) },
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (measurement.listening) Res.string.stop else Res.string.enable))
                }
                Text(stringResource(Res.string.reference), style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { controller.stop(); closed = true }) { Text(stringResource(Res.string.close)) }
            }
        }
    }
}
