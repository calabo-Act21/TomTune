package com.tomtune.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomtune.app.resources.*
import com.tomtune.kit.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.json.JSONObject

private data class KitArtwork(val bitmap: ImageBitmap, val regions: List<DrumHitRegion>)

@Composable
fun AndroidKitVisual(kit: Kit, catalogue: VisualCatalogue, modifier: Modifier, onOpen: ((String) -> Unit)?) {
    if (kit.ordered(DrumFamily.FLOOR).isEmpty()) {
        SchematicKit(kit, modifier, onOpen)
        return
    }
    val context = LocalContext.current.applicationContext
    val loader = remember(context, catalogue) {
        KitVisualLoader { requested: Kit ->
            withContext(Dispatchers.IO) {
                val visualKey = requested.visualKey()
                val manifest = JSONObject(context.assets.open("visuels/lot1.1/kit-$visualKey-zones.json")
                    .bufferedReader().use { it.readText() })
                val path = "lot1.1/${manifest.getString("image")}"
                val bitmap = context.assets.open("visuels/$path").use {
                    requireNotNull(BitmapFactory.decodeStream(it)) { "Invalid bitmap: $path" }.asImageBitmap()
                }
                val regions = run {
                    require(manifest.getString("key") == visualKey)
                    require(visualKey == "r3-f2" || manifest.getBoolean("verified"))
                    require(bitmap.width == manifest.getInt("width") && bitmap.height == manifest.getInt("height"))
                    val order = manifest.getJSONArray("hitOrder")
                    val polygons = manifest.getJSONObject("regions")
                    (0 until order.length()).map { index ->
                        val zone = order.getString(index)
                        val points = polygons.getJSONArray(zone)
                        DrumHitRegion(requireNotNull(requested.atZone(zone)).id, (0 until points.length()).map { i ->
                            val p = points.getJSONArray(i)
                            NormalPoint(p.getDouble(0).toFloat(), p.getDouble(1).toFloat())
                        })
                    }
                }
                KitArtwork(bitmap, regions)
            }
        }
    }
    LaunchedEffect(kit) { loader.load(kit) }
    val loaded by loader.state.collectAsState()
    // Recomposition precedes LaunchedEffect: never render the preceding kit in that interval.
    val frame = loaded?.takeIf { it.kit == kit }
    if (frame != null && frame.artwork == null) {
        SchematicKit(frame.kit, modifier, onOpen)
        return
    }
    val open by rememberUpdatedState(onOpen)
    BoxWithConstraints(modifier) {
        val artwork = frame?.artwork
        if (artwork == null) {
            val loading = stringResource(Res.string.kit_visual_loading)
            CircularProgressIndicator(Modifier.align(Alignment.TopCenter).padding(16.dp)
                .semantics { contentDescription = loading })
        } else {
            val bitmap = artwork.bitmap
            val fitted = fitImage(maxWidth.value, maxHeight.value, bitmap.width.toFloat(), bitmap.height.toFloat())
            Box(Modifier.fillMaxSize().pointerInput(frame, fitted, onOpen != null) {
                detectTapGestures { point ->
                    if (open != null) hitDrum(artwork.regions, fitted, point.x / density, point.y / density)?.let { open?.invoke(it) }
                }
            }) {
                Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                // Semantics only: rectangular accessibility bounds must not intercept polygon taps.
                artwork.regions.forEach { region ->
                    val drum = frame.kit.drums.first { it.id == region.drumId }
                    val rect = fitted.zone(region.bounds)
                    val label = kitDrumLabel(frame.kit, drum, accessible = true)
                    val action = stringResource(Res.string.kit_open_drum)
                    Box(Modifier.offset(rect.left.dp, rect.top.dp).size(rect.width.dp, rect.height.dp).semantics {
                        contentDescription = label
                        if (onOpen != null) {
                            role = Role.Button
                            onClick(action) { open?.invoke(drum.id); true }
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun AndroidFocusVisual(drum: Drum, catalogue: VisualCatalogue, modifier: Modifier) {
    val visual = catalogue.focusFor(drum) ?: return
    val path = "lot1.1/" + visual.path.substringAfterLast('/').replace("-vis.png", "-alpha.png")
    val context = LocalContext.current.applicationContext
    key(path) {
        var failed by remember { mutableStateOf(false) }
        val bitmap by produceState<ImageBitmap?>(null) {
            try {
                value = withContext(Dispatchers.IO) {
                    context.assets.open("visuels/$path").use { requireNotNull(BitmapFactory.decodeStream(it)).asImageBitmap() }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { failed = true }
        }
        if (failed) Text(stringResource(Res.string.kit_no_visual), Modifier.padding(horizontal = 16.dp))
        else Box(modifier, contentAlignment = Alignment.Center) {
            if (bitmap != null) Image(bitmap!!, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else CircularProgressIndicator(Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
}

@Composable
private fun SchematicKit(kit: Kit, modifier: Modifier, onOpen: ((String) -> Unit)?) {
    BoxWithConstraints(modifier) {
        val fitted = fitImage(maxWidth.value, maxHeight.value, 1000f, 800f)
        val racks = kit.ordered(DrumFamily.RACK)
        val floors = kit.ordered(DrumFamily.FLOOR)
        val positions = mutableMapOf<String, Pair<Float, Float>>()
        racks.forEachIndexed { index, drum -> positions[drum.id] = ((index + 1f) / (racks.size + 1f)) to .20f }
        floors.forEachIndexed { index, drum -> positions[drum.id] = (.75f + index * .10f) to (.46f + index * .28f) }
        kit.ordered(DrumFamily.SNARE).forEach { positions[it.id] = if (floors.isEmpty()) .19f to .66f else .18f to .62f }
        kit.ordered(DrumFamily.KICK).forEach { positions[it.id] = if (floors.isEmpty()) .60f to .66f else .46f to .65f }
        kit.visualDrums().forEach { drum ->
            val (x, y) = positions.getValue(drum.id)
            val width = fitted.width * (if (floors.isEmpty()) {
                if (drum.family == DrumFamily.KICK) .42f else .28f
            } else if (drum.family == DrumFamily.KICK) .38f else .26f)
            val height = fitted.height * (if (floors.isEmpty()) .30f else .27f)
            val label = kitDrumLabel(kit, drum, accessible = true)
            Box(Modifier.offset((fitted.left + x * fitted.width - width / 2).dp, (fitted.top + y * fitted.height - height / 2).dp)
                .size(width.dp, height.dp).background(Color(0xffe9dfd5), CircleShape).border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .then(if (onOpen == null) Modifier else Modifier.clickable(onClickLabel = stringResource(Res.string.kit_open_drum)) { onOpen(drum.id) })
                .semantics(mergeDescendants = true) { contentDescription = label; if (onOpen != null) role = Role.Button }, contentAlignment = Alignment.Center) {
                Text(drumLabel(drum), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp).clearAndSetSemantics {})
            }
        }
        Text(stringResource(if (floors.isEmpty()) Res.string.kit_schematic else Res.string.kit_visual_fallback),
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
    }
}

/** Only the inspected batter face is activated. Other faces use the real lug-count diagram. */
@Composable
fun AndroidTuningVisual(s: com.tomtune.tuning.TuningState, select: (Int) -> Unit) {
    val drum = s.drum
    if (s.head != com.tomtune.tuning.Head.BATTER || drum.family != DrumFamily.RACK || drum.diameter != 12 || drum.lugs != 6) {
        LugDiagram(s, select); return
    }
    val context = LocalContext.current.applicationContext
    val artwork by produceState<Pair<ImageBitmap, List<NormalPoint>>?>(null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val m = JSONObject(context.assets.open("visuels/lot1.1/rack-12-6-lugs.json").bufferedReader().use { it.readText() })
                require(m.getBoolean("verified") && m.getString("head") == "batter")
                val bitmap = context.assets.open("visuels/lot1.1/${m.getString("image")}").use { requireNotNull(BitmapFactory.decodeStream(it)).asImageBitmap() }
                require(bitmap.width == m.getInt("width") && bitmap.height == m.getInt("height"))
                val points = m.getJSONArray("points")
                require(points.length() == drum.lugs)
                bitmap to (0 until points.length()).map { i -> points.getJSONArray(i).let { NormalPoint(it.getDouble(0).toFloat(), it.getDouble(1).toFloat()) } }
            }.getOrNull()
        }
    }
    val loaded = artwork
    if (loaded == null) LugDiagram(s, select)
    else LugDiagram(s, select, loaded.second) { Image(loaded.first, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
}
