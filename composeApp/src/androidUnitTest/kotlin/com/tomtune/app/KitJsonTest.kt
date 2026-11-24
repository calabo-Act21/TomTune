package com.tomtune.app

import com.tomtune.kit.*
import kotlin.test.*

class KitJsonTest {
    @Test fun legacySnapshotReadsWithoutOptionalFieldsAndKeepsAllIds() {
        val legacy = """{"schema":1,"activeId":"old","kits":[{"id":"old","name":"Studio","revision":7,"drums":[
            {"id":"tom","family":"rack","order":0,"diameter":12,"lugs":6,"manualLugs":false},
            {"id":"snare","family":"snare","order":1,"diameter":14,"lugs":null,"manualLugs":false},
            {"id":"kick","family":"kick","order":2,"diameter":22,"lugs":null,"manualLugs":false}]}]}"""
        val store = KitJson.decode(legacy).validated()
        assertEquals("old", store.activeId); assertEquals(7, store.active!!.revision)
        assertEquals(listOf("tom", "snare", "kick"), store.active!!.drums.map { it.id })
        assertTrue(store.active!!.drums.all { it.reference == null && it.lastMeasurement == null })
        assertEquals(store, KitJson.decode(KitJson.encode(store)).validated())
        val original = store.active!!
        val updated = original.copy(drums = original.drums.map { if (it.id != "tom") it else it.copy(
            reference = DrumReference(123.47082531403103, TargetOrigin.MANUAL, 1234), lastMeasurement = GlobalMeasurement(120.0123, 2345)) })
        val saved = store.copy(kits = listOf(updated))
        assertEquals(saved, KitJson.decode(KitJson.encode(saved)).validated())
        assertEquals(original.drums.drop(1), saved.active!!.drums.drop(1))
    }
    @Test fun damagedOptionalValueDoesNotSilentlyResetTheStore() {
        val store = KitStore(kits = listOf(Kit("k", "K", listOf(
            Drum("t", DrumFamily.RACK, 0, 12, 6, reference = DrumReference(-1.0, TargetOrigin.SUGGESTED, 1)),
            Drum("s", DrumFamily.SNARE, 1, 14), Drum("b", DrumFamily.KICK, 2, 22)))), activeId = "k")
        assertFails { KitJson.decode(KitJson.encode(store)).validated() }
    }
}
