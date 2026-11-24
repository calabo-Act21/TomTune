package com.tomtune.app

import com.tomtune.kit.*
import org.json.JSONArray
import org.json.JSONObject

/** Schema 1 remains readable when optional tuning fields are absent. */
internal object KitJson {
    fun encode(store: KitStore): String = JSONObject().apply {
        put("schema", store.schema)
        put("activeId", store.activeId ?: JSONObject.NULL)
        put("kits", JSONArray().apply { store.kits.forEach { kit ->
            put(JSONObject().apply {
                put("id", kit.id); put("name", kit.name); put("revision", kit.revision)
                put("drums", JSONArray().apply { kit.drums.forEach { drum ->
                    put(JSONObject().apply {
                        put("id", drum.id); put("family", drum.family.key); put("order", drum.order)
                        put("diameter", drum.diameter); put("lugs", drum.lugs ?: JSONObject.NULL)
                        put("manualLugs", drum.manualLugs)
                        drum.reference?.let { value -> put("reference", JSONObject().apply {
                            put("hz", value.hz); put("origin", value.origin.name); put("timestampMillis", value.timestampMillis)
                        }) }
                        drum.lastMeasurement?.let { value -> put("lastMeasurement", JSONObject().apply {
                            put("hz", value.hz); put("origin", value.origin); put("timestampMillis", value.timestampMillis)
                        }) }
                    })
                } })
            })
        } })
    }.toString()
    fun decode(raw: String): KitStore {
        val root = JSONObject(raw)
        return KitStore(root.getInt("schema"), root.getJSONArray("kits").objects().map { kit ->
            Kit(kit.getString("id"), kit.getString("name"), kit.getJSONArray("drums").objects().map { drum ->
                Drum(drum.getString("id"), DrumFamily.entries.first { it.key == drum.getString("family") },
                    drum.getInt("order"), drum.getInt("diameter"), if (drum.isNull("lugs")) null else drum.getInt("lugs"),
                    drum.getBoolean("manualLugs"),
                    drum.optJSONObject("reference")?.let { DrumReference(it.getDouble("hz"), TargetOrigin.valueOf(it.getString("origin")), it.getLong("timestampMillis")) },
                    drum.optJSONObject("lastMeasurement")?.let { GlobalMeasurement(it.getDouble("hz"), it.getLong("timestampMillis"), it.getString("origin")) })
            }, kit.getInt("revision"))
        }, if (root.isNull("activeId")) null else root.getString("activeId"))
    }
}

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
