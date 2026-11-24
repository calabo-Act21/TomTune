package com.tomtune.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomtune.app.resources.*
import com.tomtune.kit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

@Composable
private fun assetBitmap(path: String): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            context.assets.open("visuels/$path").use { BitmapFactory.decodeStream(it).asImageBitmap() }
        }
    }
    return bitmap
}

@Composable
fun AndroidKitVisual(kit: Kit, catalogue: VisualCatalogue, modifier: Modifier, onOpen: ((String) -> Unit)?) {
    if (kit.ordered(DrumFamily.FLOOR).isEmpty()) {
        SchematicKit(kit, modifier, onOpen)
        return
    }
    val bitmap = assetBitmap("kit/${kit.visualKey()}.png")
    BoxWithConstraints(modifier) {
        if (bitmap != null) {
            Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            val fitted = fitImage(maxWidth.value, maxHeight.value, bitmap.width.toFloat(), bitmap.height.toFloat())
            catalogue.zones.getValue(kit.visualKey()).forEach { (key, zone) ->
                val drum = kit.atZone(key) ?: return@forEach
                val rect = fitted.zone(zone)
                val label = drumAccessibleLabel(drum)
                Box(Modifier.offset(rect.left.dp, rect.top.dp).size(rect.width.dp, rect.height.dp)
                    .then(if (onOpen == null) Modifier.semantics { contentDescription = label }
                        else Modifier.semantics { contentDescription = label; role = Role.Button }
                            .clickable(onClickLabel = stringResource(Res.string.kit_open_drum)) { onOpen(drum.id) }))
            }
        }
    }
}

@Composable
fun AndroidFocusVisual(drum: Drum, catalogue: VisualCatalogue, modifier: Modifier) {
    // The shared screen supplies the data card; avoid a misleading placeholder image.
    val visual = catalogue.focusFor(drum) ?: return
    val bitmap = assetBitmap(visual.path)
    if (bitmap != null) Image(bitmap, drumAccessibleLabel(drum), modifier, contentScale = ContentScale.Fit)
    else Spacer(modifier)
}

@Composable
private fun SchematicKit(kit: Kit, modifier: Modifier, onOpen: ((String) -> Unit)?) {
    BoxWithConstraints(modifier) {
        val fitted = fitImage(maxWidth.value, maxHeight.value, 1000f, 800f)
        val racks = kit.ordered(DrumFamily.RACK)
        val positions = mutableMapOf<String, Pair<Float, Float>>()
        racks.forEachIndexed { index, drum -> positions[drum.id] = ((index + 1f) / (racks.size + 1f)) to .20f }
        kit.ordered(DrumFamily.SNARE).forEach { positions[it.id] = .19f to .66f }
        kit.ordered(DrumFamily.KICK).forEach { positions[it.id] = .60f to .66f }
        kit.visualDrums().forEach { drum ->
            val (x, y) = positions.getValue(drum.id)
            val width = fitted.width * (if (drum.family == DrumFamily.KICK) .42f else .28f)
            val height = fitted.height * .30f
            val label = drumAccessibleLabel(drum)
            Box(Modifier.offset((fitted.left + x * fitted.width - width / 2).dp, (fitted.top + y * fitted.height - height / 2).dp)
                .size(width.dp, height.dp).background(Color(0xffe9dfd5), CircleShape).border(3.dp, Color(0xff752c3e), CircleShape)
                .then(if (onOpen == null) Modifier else Modifier.clickable(onClickLabel = stringResource(Res.string.kit_open_drum)) { onOpen(drum.id) })
                .semantics(mergeDescendants = true) { contentDescription = label; if (onOpen != null) role = Role.Button }, contentAlignment = Alignment.Center) {
                Text(drumLabel(drum), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(4.dp).clearAndSetSemantics {})
            }
        }
        Text(stringResource(Res.string.kit_schematic), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
    }
}
