package com.tomtune.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomtune.app.resources.*
import com.tomtune.contracts.CaptureIssue
import com.tomtune.kit.*
import com.tomtune.tuning.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun pitchLabel(hz: Double): String {
    val midi = nearestMidi(hz)
    val names = stringResource(Res.string.tune_note_names).split("|")
    return "${names[((midi % 12) + 12) % 12]}${midi / 12 - 1} · ${formatHz(hz)} Hz"
}

@Composable
fun DrumMeasurements(drum: Drum) {
    Text(stringResource(if (drum.lastMeasurement == null) Res.string.tune_never else Res.string.tune_done))
    Text(stringResource(Res.string.tune_reference) + " · " + (drum.reference?.let { pitchLabel(it.hz) } ?: stringResource(Res.string.tune_undefined)))
    Text(stringResource(Res.string.tune_last) + " · " + (drum.lastMeasurement?.let { pitchLabel(it.hz) } ?: stringResource(Res.string.tune_unmeasured)))
    if (drum.reference != null && drum.lastMeasurement != null)
        Text(stringResource(Res.string.tune_delta_reference, formatHz(cents(drum.lastMeasurement!!.hz, drum.reference!!.hz))))
}

@Composable
fun TuningScreen(s: TuningState, controller: TuningController,
    headVisual: @Composable (TuningState, (Int) -> Unit) -> Unit = { state, select -> LugDiagram(state, select) }) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = controller::requestAbandon, enabled = !s.saving) { Text("← " + stringResource(Res.string.tune_back_drum)) }
        Text(drumLabel(s.drum), style = MaterialTheme.typography.titleLarge)
        when (s.stage) {
            TuningStage.TARGET -> {
                Text(stringResource(Res.string.tune_global_target), style = MaterialTheme.typography.headlineSmall)
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(pitchLabel(s.targetHz) + " ▾") }
                    DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                        (28..60).forEach { midi -> DropdownMenuItem(text = { Text(pitchLabel(midiHz(midi))) },
                            onClick = { expanded = false; controller.chooseMidi(midi) }) }
                    }
                }
                Text(stringResource(if (s.fromReference) Res.string.tune_source_reference else if (s.targetOrigin == TargetOrigin.SUGGESTED)
                    Res.string.tune_source_suggestion else Res.string.tune_source_manual))
                Text(stringResource(Res.string.tune_profile))
                Text(stringResource(Res.string.tune_setup_help))
                Button(onClick = controller::start, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.tune_begin_batter)) }
            }
            TuningStage.LUGS, TuningStage.ADVICE -> {
                Text(stringResource(if (s.head == Head.BATTER) Res.string.tune_batter else Res.string.tune_resonant), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.tune_pass, s.pass))
                Text(stringResource(Res.string.tune_local_target, formatHz(s.localTarget)))
                Text(stringResource(if (s.head == Head.BATTER) Res.string.tune_batter_help else Res.string.tune_resonant_help))
                headVisual(s, controller::selectLug)
                Text(stringResource(Res.string.tune_progress, s.measurements.size, s.drum.lugs!!))
                if (s.stage == TuningStage.LUGS) {
                    Text(stringResource(Res.string.tune_active_lug, s.lug + 1, s.drum.lugs!!))
                    CaptureFeedback(s, controller)
                } else {
                    Text(stringResource(Res.string.tune_advice_help))
                    s.measurements.toSortedMap().forEach { (index, hz) ->
                        val a = advice(hz, s.localTarget, s.profile.localToleranceCents)
                        Text(stringResource(Res.string.tune_lug_value, index + 1, formatHz(hz)) + " · " + adviceLabel(a), color = adviceColor(a))
                    }
                    Button(onClick = controller::repeatHead, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.tune_repeat_head)) }
                    OutlinedButton(onClick = controller::nextHead, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (s.head == Head.BATTER) Res.string.tune_next_resonant else Res.string.tune_next_center))
                    }
                }
                s.lastConfirmed?.let { (index, hz) -> Text(stringResource(Res.string.tune_confirmed_lug, index + 1, formatHz(hz)),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            }
            TuningStage.CENTER -> {
                Text(stringResource(Res.string.tune_next_center), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.tune_center_help))
                Text(stringResource(Res.string.tune_global_target) + " · " + pitchLabel(s.targetHz))
                CaptureFeedback(s, controller)
                val hz = s.centerHz
                if (hz != null) {
                    Text(pitchLabel(hz), style = MaterialTheme.typography.headlineMedium)
                    val reference = s.drum.reference
                    Text(stringResource(if (reference?.hz == s.targetHz) Res.string.tune_delta_common else Res.string.tune_delta_target,
                        formatHz(cents(hz, s.targetHz))))
                    if (reference == null) Text(stringResource(Res.string.tune_no_reference))
                    else if (reference.hz != s.targetHz) Text(stringResource(Res.string.tune_delta_reference, formatHz(cents(hz, reference.hz))))
                    if (s.needsCorrection) Text(stringResource(Res.string.tune_correction_help))
                    OutlinedButton(onClick = controller::remeasureCenter, enabled = !s.saving, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.tune_remeasure)) }
                    if (s.needsCorrection) OutlinedButton(onClick = controller::correct, enabled = !s.saving, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.tune_correct)) }
                    if (s.writeFailed) Text(stringResource(Res.string.kit_write_error), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Button(onClick = { controller.finish(false) }, enabled = !s.saving, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.tune_finish)) }
                    OutlinedButton(onClick = { controller.finish(true) }, enabled = !s.saving, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.tune_finish_reference) + "\n" + pitchLabel(s.targetHz))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    if (s.abandonDialog) AlertDialog(onDismissRequest = controller::cancelAbandon,
        title = { Text(stringResource(Res.string.tune_abandon_title)) }, text = { Text(stringResource(Res.string.tune_abandon_body)) },
        confirmButton = { TextButton(onClick = controller::abandon) { Text(stringResource(Res.string.tune_abandon)) } },
        dismissButton = { TextButton(onClick = controller::cancelAbandon) { Text(stringResource(Res.string.tune_keep)) } })
}

@Composable
private fun CaptureFeedback(s: TuningState, controller: TuningController) {
    Card(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(when (s.issue) {
                CaptureIssue.PermissionDenied -> Res.string.denied
                CaptureIssue.Interrupted -> Res.string.interrupted
                CaptureIssue.RouteChanged -> Res.string.route
                CaptureIssue.Unavailable -> Res.string.unavailable
                CaptureIssue.Overrun -> Res.string.overrun
                null -> when(s.feedback) {
                    Feedback.WAITING -> Res.string.waiting
                    Feedback.DETECTED -> Res.string.detected
                    Feedback.PROVISIONAL -> Res.string.provisional
                    Feedback.REJECTED -> Res.string.rejected
                    Feedback.UNSTABLE -> Res.string.tune_unstable
                    Feedback.CONFIRMED -> Res.string.tune_confirmed
                }
            }))
            if (s.provisionalHz != null) {
                Text(if (s.stage == TuningStage.CENTER) pitchLabel(s.provisionalHz!!) else "${formatHz(s.provisionalHz!!)} Hz", style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.tune_delta_target, formatHz(cents(s.provisionalHz!!,
                    if (s.stage == TuningStage.CENTER) s.targetHz else s.localTarget))))
                Text(stringResource(Res.string.tune_series, s.seriesCount))
            }
            if (s.feedback == Feedback.REJECTED) {
                s.rejection?.let { reason -> Text(stringResource(when (reason) {
                    com.tomtune.contracts.RejectionReason.Clipped -> Res.string.tune_clipped
                    com.tomtune.contracts.RejectionReason.Noise -> Res.string.tune_noise
                    com.tomtune.contracts.RejectionReason.AmbiguousModes -> Res.string.tune_ambiguous
                    com.tomtune.contracts.RejectionReason.TooShort -> Res.string.tune_short
                })) }
                Text(stringResource(Res.string.tune_restrike))
            }
            if (!s.listening && s.centerHz == null) Button(onClick = controller::resume) { Text(stringResource(Res.string.tune_resume)) }
        }
    }
}

@Composable
private fun adviceLabel(a: Advice): String = when(a) {
    Advice.TIGHTEN -> "+ " + stringResource(Res.string.tune_tighten)
    Advice.LOOSEN -> "− " + stringResource(Res.string.tune_loosen)
    Advice.LEAVE -> "✓ " + stringResource(Res.string.tune_leave)
}
private fun adviceColor(a: Advice) = when(a) {
    Advice.TIGHTEN -> Color(0xff8b3a18)
    Advice.LOOSEN -> Color(0xff23578a)
    Advice.LEAVE -> Color(0xff286140)
}

/** Shared clockwise geometry: arrow, touch target and semantics use the very same point.
 * No photograph is used without a face-specific verified manifest. */
@Composable
fun LugDiagram(s: TuningState, select: (Int) -> Unit,
    anchors: List<NormalPoint> = emptyList(), photo: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(1f)) {
            val width = maxWidth
            if (photo != null) photo() else Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xfff7ede4), radius = size.width * .35f)
                drawCircle(Color(0xff602033), radius = size.width * .35f, style = Stroke(3.dp.toPx()))
            }
            if (photo == null) Text("TomTune", Modifier.align(Alignment.TopCenter).padding(top = width * .03f), color = MaterialTheme.colorScheme.primary)
            if (photo == null) Text(stringResource(if (s.head == Head.BATTER) Res.string.tune_batter else Res.string.tune_resonant),
                Modifier.align(Alignment.Center).width(width * .40f), textAlign = TextAlign.Center)
            val points = anchors.takeIf { it.size == s.drum.lugs } ?: schematicLugAnchors(s.drum.lugs!!)
            for ((index, point) in points.withIndex()) {
                val x = point.x
                val y = point.y
                val active = s.stage == TuningStage.LUGS && index == s.lug
                val hz = s.measurements[index]
                val status = if (hz == null) stringResource(Res.string.tune_unmeasured) else
                    stringResource(Res.string.tune_confirmed) + " · ${formatHz(hz)} Hz · " + adviceLabel(advice(hz, s.localTarget, s.profile.localToleranceCents))
                val label = stringResource(Res.string.lug, index + 1) + " · " + status
                Box(Modifier.offset(width * x - 24.dp, width * y - 24.dp).size(48.dp)
                    .background(if (active) MaterialTheme.colorScheme.primary else Color(0xfffff9f3), CircleShape)
                    .border(if (hz != null) 3.dp else 1.dp, if (hz != null) adviceColor(advice(hz, s.localTarget, s.profile.localToleranceCents)) else MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(onClickLabel = stringResource(Res.string.tune_select_lug)) { select(index) }
                    .semantics(mergeDescendants = true) { contentDescription = label; selected = active; role = Role.Button }, contentAlignment = Alignment.Center) {
                    Text("${index + 1}", color = if (active) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.clearAndSetSemantics {})
                }
                if (active) Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                    val at = Offset(size.width * x, size.height * y)
                    val color = Color(0xff602033)
                    drawLine(color, at - Offset(0f, 54.dp.toPx()), at - Offset(0f, 8.dp.toPx()), strokeWidth = 3.dp.toPx())
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(at.x, at.y); lineTo(at.x - 7.dp.toPx(), at.y - 12.dp.toPx())
                        lineTo(at.x + 7.dp.toPx(), at.y - 12.dp.toPx()); close()
                    }
                    drawPath(path, color)
                }
            }
        }
    }
}
