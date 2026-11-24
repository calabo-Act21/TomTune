package com.tomtune.app

import android.content.Context
import android.system.Os
import com.tomtune.kit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

/** App-private, versioned snapshot. A synced sibling is renamed atomically over the prior snapshot. */
class AndroidKitRepository(context: Context) : KitRepository {
    private val file = File(context.filesDir, "kits-v1.json")
    private val mutex = Mutex()
    override suspend fun load(): KitStore = withContext(Dispatchers.IO) {
        mutex.withLock {
            val bytes = try { file.inputStream().use { it.readBytes() } } catch (e: FileNotFoundException) {
                // Missing is first launch; an existing unreadable document must never be reset silently.
                if (file.exists()) throw e
                return@withLock KitStore()
            }
            KitJson.decode(String(bytes, Charsets.UTF_8)).validated()
        }
    }
    override suspend fun write(store: KitStore) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val bytes = KitJson.encode(store.validated()).toByteArray(Charsets.UTF_8)
            val pending = File(file.parentFile, file.name + ".new")
            try {
                FileOutputStream(pending).use { stream -> stream.write(bytes); stream.fd.sync() }
                // Unlike AtomicFile.finishWrite, Os.rename reports a failed commit to the caller.
                Os.rename(pending.path, file.path)
            } finally { pending.delete() }
        }
    }
}

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
fun readVisualCatalogue(context: Context): VisualCatalogue {
    val assets = context.assets
    val focus = JSONArray(assets.open("visuels/catalogue-focus.json").bufferedReader().use { it.readText() })
        .objects().map { item -> FocusVisual(DrumFamily.entries.first { it.key == item.getString("type") },
            item.getInt("diametrePouces"), item.getInt("visParPeau"),
            // The handoff names JPEGs, but all supplied files are PNGs.
            item.getString("fichier").substringBeforeLast('.') + ".png",
            item.getJSONArray("points").let { points -> (0 until points.length()).map { i ->
                val p = points.getJSONArray(i); NormalPoint(p.getDouble(0).toFloat(), p.getDouble(1).toFloat())
            } }) }
    val json = JSONObject(assets.open("visuels/zones-kit.json").bufferedReader().use { it.readText() })
    val zones = json.keys().asSequence().associateWith { key ->
        val kit = json.getJSONObject(key)
        kit.keys().asSequence().associateWith { zone ->
            val r = kit.getJSONArray(zone)
            NormalRect(r.getDouble(0).toFloat(), r.getDouble(1).toFloat(), r.getDouble(2).toFloat(), r.getDouble(3).toFloat())
        }
    }
    return VisualCatalogue(focus, zones)
}
