package com.tomtune.app

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tomtune.app.resources.*
import com.tomtune.kit.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun drumLabel(drum: Drum): String = stringResource(when (drum.family) {
    DrumFamily.RACK -> Res.string.kit_rack
    DrumFamily.FLOOR -> Res.string.kit_floor
    DrumFamily.SNARE -> Res.string.kit_snare
    DrumFamily.KICK -> Res.string.kit_kick
}) + " ${drum.diameter}″"

@Composable
fun drumAccessibleLabel(drum: Drum): String = drumLabel(drum) +
    (drum.lugs?.let { " · " + stringResource(Res.string.kit_lug_count, it) } ?: "")

@Composable
fun KitApp(controller: KitController, catalogue: VisualCatalogue,
    kitVisual: @Composable (Kit, Modifier, ((String) -> Unit)?) -> Unit,
    focusVisual: @Composable (Drum, Modifier) -> Unit) {
    val state by controller.state.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xff752c3e),
        background = Color(0xfff6f2ed), surface = Color(0xfffffbf7))) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 16.dp)) {
                Text("TomTune", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
                when (state.screen) {
                    KitScreen.LOADING -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    KitScreen.LOAD_ERROR -> {
                        Text(stringResource(Res.string.kit_load_error), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        Button(onClick = controller::load) { Text(stringResource(Res.string.kit_retry)) }
                    }
                    KitScreen.WELCOME -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(Res.string.kit_welcome), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(Res.string.kit_welcome_body))
                        Button(onClick = { controller.configure() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.kit_define)) }
                    }
                    KitScreen.CONFIGURE -> {
                        BackAction(state.busy, controller::back, Res.string.kit_cancel)
                        Text(stringResource(Res.string.kit_configure), style = MaterialTheme.typography.headlineMedium)
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(Res.string.kit_setup_help), modifier = Modifier.padding(top = 12.dp))
                            val draft = state.draft!!
                            CountChoice(Res.string.kit_racks, 3, draft.ordered(DrumFamily.RACK).size) { controller.count(DrumFamily.RACK, it) }
                            CountChoice(Res.string.kit_floors, 2, draft.ordered(DrumFamily.FLOOR).size) { controller.count(DrumFamily.FLOOR, it) }
                            draft.drums.sortedWith(compareBy<Drum> { it.family.ordinal }.thenBy { it.order }).forEach { drum ->
                                key(drum.id) {
                                    Card(Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(drumLabel(drum), style = MaterialTheme.typography.titleMedium)
                                            Choice(stringResource(Res.string.kit_diameter), "${drum.diameter}″", drum.family.diameters,
                                                { "$it″" }) { controller.diameter(drum.id, it) }
                                            if (drum.family.isTom) {
                                                val covered = catalogue.lugChoices(drum.family, drum.diameter)
                                                Choice(stringResource(Res.string.kit_lugs), drum.lugs.toString(), covered,
                                                    { it.toString() }, (3..12).filter { it !in covered },
                                                    stringResource(Res.string.kit_custom_lugs)) { controller.lugs(drum.id, it) }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Button(onClick = controller::preview, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text(stringResource(Res.string.kit_see)) }
                    }
                    KitScreen.PREVIEW -> {
                        BackAction(state.busy, controller::back, Res.string.kit_configuration)
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(Res.string.kit_preview), style = MaterialTheme.typography.headlineMedium)
                            kitVisual(state.draft!!, Modifier.fillMaxWidth().height(260.dp), null)
                            OutlinedTextField(value = state.draft!!.name, onValueChange = controller::name,
                                enabled = !state.busy, label = { Text(stringResource(Res.string.kit_name)) },
                                singleLine = true, isError = state.error != null && state.error != KitError.WRITE,
                                supportingText = { if (state.error != null && state.error != KitError.WRITE) KitErrorText(state.error!!) else Text(stringResource(Res.string.kit_name_hint)) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); controller.save() }), modifier = Modifier.fillMaxWidth())
                            if (state.error == KitError.WRITE) KitErrorText(KitError.WRITE)
                            Button(onClick = { keyboard?.hide(); controller.save() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(if (state.busy) Res.string.kit_saving else Res.string.kit_save))
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    KitScreen.HOME -> {
                        KitToolbar(state, controller)
                        if (state.error != null) KitErrorText(state.error!!)
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        kitVisual(state.store.active!!, Modifier.weight(1f).fillMaxWidth(), if (state.busy) null else controller::openDrum)
                        Text(stringResource(Res.string.kit_touch_help), style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 12.dp))
                    }
                    KitScreen.DRUM -> {
                        BackAction(false, controller::back, Res.string.kit_back_home)
                        val drum = state.store.active!!.drums.first { it.id == state.selectedDrumId }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(drumLabel(drum), style = MaterialTheme.typography.headlineMedium)
                            focusVisual(drum, Modifier.fillMaxWidth().height(320.dp))
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(drumLabel(drum), style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(Res.string.kit_diameter_value, drum.diameter))
                                    drum.lugs?.let { Text(stringResource(Res.string.kit_lug_count, it)) }
                                    if (drum.family == DrumFamily.SNARE && drum.diameter == 14) Text(stringResource(Res.string.kit_snare_illustration))
                                    if (catalogue.focusFor(drum) == null) Text(stringResource(Res.string.kit_no_visual))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackAction(busy: Boolean, back: () -> Unit, label: org.jetbrains.compose.resources.StringResource) {
    TextButton(onClick = back, enabled = !busy) { Text("← " + stringResource(label)) }
}

@Composable
private fun KitErrorText(error: KitError) {
    Text(stringResource(when (error) {
        KitError.NAME_EMPTY -> Res.string.kit_empty_name
        KitError.NAME_LONG -> Res.string.kit_long_name
        KitError.NAME_DUPLICATE -> Res.string.kit_duplicate_name
        KitError.WRITE -> Res.string.kit_write_error
    }), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun CountChoice(label: org.jetbrains.compose.resources.StringResource, max: Int, selected: Int, choose: (Int) -> Unit) {
    Column {
        val name = stringResource(label)
        Text(name, style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..max).forEach { count ->
                FilterChip(selected = selected == count, onClick = { choose(count) }, label = { Text("$count") },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "$name : $count" })
            }
        }
    }
}

@Composable
private fun Choice(label: String, value: String, values: List<Int>, format: (Int) -> String,
    custom: List<Int> = emptyList(), customLabel: String = "", choose: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label : $value ▾") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { item -> DropdownMenuItem(text = { Text(format(item)) }, onClick = { expanded = false; choose(item) }) }
            if (custom.isNotEmpty()) {
                HorizontalDivider()
                Text(customLabel, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(12.dp))
                custom.forEach { item -> DropdownMenuItem(text = { Text(format(item)) }, onClick = { expanded = false; choose(item) }) }
            }
        }
    }
}

@Composable
private fun KitToolbar(state: KitState, controller: KitController) {
    var kitsOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            val selector = stringResource(Res.string.kit_selector)
            OutlinedButton(onClick = { kitsOpen = true }, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = selector + ": " + state.store.active!!.name }) {
                Text(state.store.active!!.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(" ▾")
            }
            DropdownMenu(expanded = kitsOpen, onDismissRequest = { kitsOpen = false }) {
                state.store.kits.forEach { kit -> DropdownMenuItem(text = { Text(kit.name) }, onClick = { kitsOpen = false; controller.select(kit.id) },
                    modifier = Modifier.semantics { selected = kit.id == state.store.activeId }) }
            }
        }
        Box {
            val menu = stringResource(Res.string.kit_menu)
            OutlinedButton(onClick = { menuOpen = true }, enabled = !state.busy, contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(48.dp).semantics { contentDescription = menu }) { Text("☰") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(Res.string.kit_check) + " · " + stringResource(Res.string.kit_coming)) }, onClick = {}, enabled = false)
                DropdownMenuItem(text = { Text(stringResource(Res.string.kit_references) + " · " + stringResource(Res.string.kit_coming)) }, onClick = {}, enabled = false)
                HorizontalDivider()
                DropdownMenuItem(text = { Text(stringResource(Res.string.kit_edit)) }, onClick = { menuOpen = false; controller.configure(edit = true) })
                DropdownMenuItem(text = { Text(stringResource(Res.string.kit_create)) }, onClick = { menuOpen = false; controller.configure() })
            }
        }
    }
}
