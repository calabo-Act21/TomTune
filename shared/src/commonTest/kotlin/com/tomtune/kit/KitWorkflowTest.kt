package com.tomtune.kit

import kotlin.test.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*

/** Targeted regressions: draft isolation, atomic snapshot semantics and image/ID association. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class KitWorkflowTest {
    private var serial = 0
    private val catalogue = VisualCatalogue(listOf(
        FocusVisual(DrumFamily.RACK, 12, 6, "rack6.png"),
        FocusVisual(DrumFamily.RACK, 12, 8, "rack8.png"),
        FocusVisual(DrumFamily.SNARE, 14, 8, "snare8.png")
    ), emptyMap())
    private fun rules() = KitRules({ "id-${serial++}" }, catalogue)
    private class MemoryRepository : KitRepository {
        var disk = KitStore()
        var fail = false
        var gate: CompletableDeferred<Unit>? = null
        var writes = 0
        override suspend fun load() = disk
        override suspend fun write(store: KitStore) {
            writes++
            gate?.await()
            if (fail) error("disk full")
            disk = store
        }
    }

    @Test fun countDiameterAndManualLugsKeepStableDrums() {
        val rules = rules()
        var kit = rules.fresh()
        val original = kit.ordered(DrumFamily.RACK).single()
        assertEquals(12, original.diameter)
        assertEquals(6, original.lugs)
        kit = rules.count(kit, DrumFamily.RACK, 3)
        assertEquals(listOf(10, 12, 13), kit.ordered(DrumFamily.RACK).map { it.diameter })
        assertEquals(original, kit.atZone("rack2"))
        kit = rules.count(kit, DrumFamily.RACK, 1)
        assertEquals(original, kit.ordered(DrumFamily.RACK).single())
        kit = rules.diameter(kit, original.id, 10)
        assertEquals(5, kit.atZone("rack1")!!.lugs)
        kit = rules.lugs(kit, original.id, 9)
        kit = rules.diameter(kit, original.id, 14)
        assertEquals(9, kit.atZone("rack1")!!.lugs)
        assertEquals(original.id, kit.atZone("rack1")!!.id)
        assertNull(catalogue.focusFor(kit.atZone("rack1")!!))
    }

    @Test fun exactFocusAndLetterboxZones() {
        val kit = rules().fresh()
        val rack = kit.atZone("rack1")!!
        assertEquals("rack6.png", catalogue.focusFor(rack)?.path)
        assertEquals("rack8.png", catalogue.focusFor(rack.copy(lugs = 8))?.path)
        assertNull(catalogue.focusFor(rack.copy(lugs = 7)))
        assertEquals("snare8.png", catalogue.focusFor(kit.atZone("snare")!!)?.path)
        val portrait = fitImage(360f, 600f, 1536f, 1024f)
        assertEquals(ImageRect(0f, 180f, 360f, 240f), portrait)
        val zone = portrait.zone(NormalRect(.25f, .25f, .5f, .5f))
        assertEquals(ImageRect(90f, 240f, 90f, 60f), zone)
        assertEquals(ImageRect(240f, 0f, 720f, 480f), fitImage(1200f, 480f, 1536f, 1024f))
        val tie = rack.copy(id = "tie", order = rack.order + 10)
        val sorted = kit.copy(drums = kit.drums + tie)
        assertEquals(rack.id, sorted.atZone("rack1")!!.id)
        assertEquals(tie.id, sorted.atZone("rack2")!!.id)
    }

    @Test fun failedSaveRetainsDraftAndDoubleTapWritesOnce() = runTest {
        val repository = MemoryRepository()
        val controller = KitController(repository, rules(), backgroundScope)
        runCurrent()
        controller.configure()
        controller.count(DrumFamily.RACK, 3)
        controller.name("  Studio  ")
        controller.preview()
        controller.back()
        assertEquals(3, controller.state.value.draft!!.ordered(DrumFamily.RACK).size)
        assertEquals("  Studio  ", controller.state.value.draft!!.name)
        controller.preview()
        repository.fail = true
        controller.save()
        runCurrent()
        assertEquals(KitError.WRITE, controller.state.value.error)
        assertEquals("  Studio  ", controller.state.value.draft!!.name)
        assertEquals(KitStore(), repository.disk)
        repository.fail = false
        repository.gate = CompletableDeferred()
        controller.save()
        controller.save()
        runCurrent()
        assertTrue(controller.state.value.busy)
        assertEquals(2, repository.writes) // One failed attempt and one pending retry.
        repository.gate!!.complete(Unit)
        runCurrent()
        assertEquals(KitScreen.HOME, controller.state.value.screen)
        assertEquals("Studio", repository.disk.active!!.name)
        assertEquals(1, repository.disk.kits.size)
    }

    @Test fun namesCancelEditSelectAndReload() = runTest {
        val repository = MemoryRepository()
        val controller = KitController(repository, rules(), backgroundScope)
        runCurrent()
        controller.configure()
        controller.preview()
        controller.name("   "); controller.save()
        assertEquals(KitError.NAME_EMPTY, controller.state.value.error)
        controller.name("a".repeat(61)); controller.save()
        assertEquals(KitError.NAME_LONG, controller.state.value.error)
        controller.name(" Studio "); controller.save(); runCurrent()
        val first = repository.disk.active!!
        controller.configure()
        controller.preview()
        controller.name("sTUDIO"); controller.save()
        assertEquals(KitError.NAME_DUPLICATE, controller.state.value.error)
        controller.name("Live"); controller.save(); runCurrent()
        val second = repository.disk.active!!
        assertEquals(2, repository.disk.kits.size)
        controller.select(first.id); runCurrent()
        controller.configure(edit = true)
        controller.diameter(first.atZone("rack1")!!.id, 14)
        controller.back()
        assertEquals(first, repository.disk.active)
        controller.configure(edit = true)
        controller.name("Studio II"); controller.preview(); controller.save(); runCurrent()
        assertEquals(first.id, repository.disk.activeId)
        assertEquals(first.drums, repository.disk.active!!.drums)
        assertEquals(first.revision + 1, repository.disk.active!!.revision)
        assertEquals(second, repository.disk.kits.last())
        repository.fail = true
        controller.select(second.id); runCurrent()
        assertEquals(first.id, controller.state.value.store.activeId)
        repository.fail = false
        controller.select(second.id); runCurrent()
        val relaunched = KitController(repository, rules(), backgroundScope)
        runCurrent()
        assertEquals(second, relaunched.state.value.store.active)
        assertEquals(2, relaunched.state.value.store.kits.size)
        assertEquals(KitScreen.HOME, relaunched.state.value.screen)
    }

    @Test fun unreadableStoreNeverOpensAnEmptyEditableState() = runTest {
        val repository = object : KitRepository {
            override suspend fun load(): KitStore = error("unsupported or corrupt document")
            override suspend fun write(store: KitStore) { error("must not write") }
        }
        val controller = KitController(repository, rules(), backgroundScope)
        runCurrent()
        assertEquals(KitScreen.LOAD_ERROR, controller.state.value.screen)
        assertFalse(controller.state.value.busy)
    }
}
