package com.tomtune.kit

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class KitPresentationTest {
    private fun kit(id: String) = Kit(id, id, listOf(Drum("$id-rack", DrumFamily.RACK, 0, 12, 6)))

    @Test fun lateDecodeCannotReplaceCurrentImageOrIdsEvenForSameVisualKey() = runTest {
        val a = kit("A")
        val b = kit("B") // same composition, different drum IDs
        val gate = CompletableDeferred<Unit>()
        data class Artwork(val bitmap: String, val drumIds: List<String>)
        val loader = KitVisualLoader { requested: Kit ->
            if (requested.id == "A") gate.await()
            Artwork("bitmap-${requested.id}", requested.drums.map { it.id })
        }
        val old = launch { loader.load(a) }
        runCurrent()
        assertNull(loader.state.value) // no zones during loading
        // Leave the old request alive: correctness must not depend on decoder cancellation.
        val current = launch { loader.load(b) }
        runCurrent()
        current.join()
        assertEquals(b, loader.state.value?.kit)
        assertEquals(Artwork("bitmap-B", listOf("B-rack")), loader.state.value?.artwork)
        gate.complete(Unit)
        old.join()
        assertEquals(b, loader.state.value?.kit)
        assertEquals(Artwork("bitmap-B", listOf("B-rack")), loader.state.value?.artwork)
    }

    @Test fun loadingClearsPriorFrameAndFailureFallsBackToRequestedKit() = runTest {
        val gate = CompletableDeferred<Unit>()
        val loader = KitVisualLoader { requested: Kit ->
            if (requested.id == "B") { gate.await(); error("missing asset") }
            "bitmap-A"
        }
        loader.load(kit("A"))
        assertNotNull(loader.state.value?.artwork)
        val pending = launch { loader.load(kit("B")) }
        runCurrent()
        assertNull(loader.state.value)
        gate.complete(Unit)
        pending.join()
        assertEquals(kit("B"), loader.state.value?.kit)
        assertNull(loader.state.value?.artwork)
    }

    @Test fun frontPolygonWinsAndTouchesFollowActualFitRectangle() {
        fun box(id: String, l: Float, t: Float, r: Float, b: Float) = DrumHitRegion(id,
            listOf(NormalPoint(l,t), NormalPoint(r,t), NormalPoint(r,b), NormalPoint(l,b)))
        val regions = listOf(box("front", .6f,.3f,.9f,.8f), box("back", .5f,.2f,.7f,.6f))
        listOf(fitImage(320f, 500f, 1536f, 1024f), fitImage(1000f, 300f, 1536f, 1024f)).forEach { image ->
            assertEquals("front", hitDrum(regions, image, image.left + .65f*image.width, image.top + .4f*image.height))
            assertEquals("back", hitDrum(regions, image, image.left + .55f*image.width, image.top + .4f*image.height))
            assertNull(hitDrum(regions, image, image.left - 1, image.top))
            assertNull(hitDrum(regions, image, image.left + .1f*image.width, image.top + .1f*image.height))
        }
    }
}
