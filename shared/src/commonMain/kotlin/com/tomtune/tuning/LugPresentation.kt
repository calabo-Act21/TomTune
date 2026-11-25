package com.tomtune.tuning

import com.tomtune.kit.NormalPoint
import kotlin.math.*

/** A number above the head leaves the hardware visible: outside the upper rim,
 * on the skin above lower heads. Both select the same lug. */
data class LugMarker(val anchor: NormalPoint, val number: NormalPoint)

fun lugMarkers(anchors: List<NormalPoint>): List<LugMarker> {
    require(anchors.size in 3..12)
    val middleY = (anchors.minOf { it.y } + anchors.maxOf { it.y }) / 2
    return anchors.map { p ->
        require(p.x in 0f..1f && p.y in 0f..1f)
        // Bring lower side numbers inside the skin instead of straddling its curved edge.
        val inward = if (p.y > middleY && abs(p.x - .5f) > .28f) {
            if (p.x > .5f) -.045f else .045f
        } else 0f
        LugMarker(p, NormalPoint(p.x + inward, p.y - .095f))
    }
}

/** Use the image's actual square and top inset, in the same units as the touch coordinates.
 * Accessibility bounds encompass these two targets; ordinary taps resolve nearest distance. */
fun hitLug(markers: List<LugMarker>, imageSize: Float, topInset: Float,
    x: Float, y: Float, radius: Float): Int? {
    if (imageSize <= 0f || radius <= 0f) return null
    return markers.indices.map { index ->
        index to listOf(markers[index].anchor, markers[index].number).minOf { p ->
            hypot((x - p.x * imageSize).toDouble(), (y - topInset - p.y * imageSize).toDouble())
        }
    }.filter { it.second <= radius }.minByOrNull { it.second }?.first
}
