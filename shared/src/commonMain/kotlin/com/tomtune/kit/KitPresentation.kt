package com.tomtune.kit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A single publication binds artwork (including its regions) to an immutable kit snapshot. */
data class KitVisualFrame<T>(val kit: Kit, val artwork: T?)

class KitVisualLoader<T>(private val decode: suspend (Kit) -> T) {
    private var generation = 0L
    private val mutable = MutableStateFlow<KitVisualFrame<T>?>(null)
    val state = mutable.asStateFlow()

    /** null is loading, a frame with null artwork requests this kit's schematic fallback. */
    suspend fun load(kit: Kit) {
        val request = ++generation
        mutable.value = null
        val artwork = try { decode(kit) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
        // Also protects against non-cooperative decoders and A → B → A transitions.
        if (request == generation) mutable.value = KitVisualFrame(kit, artwork)
    }
}

data class NormalPoint(val x: Float, val y: Float)
data class DrumHitRegion(val drumId: String, val points: List<NormalPoint>) {
    val bounds get() = NormalRect(points.minOf { it.x }, points.minOf { it.y },
        points.maxOf { it.x }, points.maxOf { it.y })

    fun contains(x: Float, y: Float): Boolean {
        var inside = false
        var previous = points.last()
        for (point in points) {
            if ((point.y > y) != (previous.y > y) &&
                x < (previous.x - point.x) * (y - point.y) / (previous.y - point.y) + point.x) inside = !inside
            previous = point
        }
        return inside
    }
}

/** Regions are front-to-back. Coordinates and accessibility bounds share the bitmap's Fit. */
fun hitDrum(regions: List<DrumHitRegion>, image: ImageRect, x: Float, y: Float): String? {
    if (image.width <= 0 || image.height <= 0) return null
    val nx = (x - image.left) / image.width
    val ny = (y - image.top) / image.height
    if (nx !in 0f..1f || ny !in 0f..1f) return null
    return regions.firstOrNull { it.contains(nx, ny) }?.drumId
}
