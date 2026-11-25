package com.tomtune.tuning

import com.tomtune.kit.NormalPoint
import kotlin.test.*

class LugPresentationTest {
    private val floor = listOf(NormalPoint(.646f,.058f), NormalPoint(.813f,.196f),
        NormalPoint(.813f,.373f), NormalPoint(.638f,.516f), NormalPoint(.356f,.516f),
        NormalPoint(.176f,.371f), NormalPoint(.180f,.195f), NormalPoint(.355f,.057f))

    @Test fun numbersAreAboveHeadsAndBothTargetsSelectTheSameLugAfterScaling() {
        val all = listOf(floor) + (3..12).map { schematicLugAnchors(it) }
        for (points in all) {
            val markers = lugMarkers(points)
            for (size in listOf(260f, 320f, 360f, 900f)) {
                val scale = size / 360f
                val inset = 48f * scale
                val radius = 24f * scale
                markers.forEachIndexed { index, m ->
                    assertTrue(kotlin.math.abs(m.anchor.x - m.number.x) <= .05f)
                    assertTrue(m.number.y < m.anchor.y)
                    for (p in listOf(m.anchor, m.number))
                        assertEquals(index, hitLug(markers, size, inset, p.x * size, inset + p.y * size, radius))
                    assertEquals(index, hitLug(markers, size, inset, (m.anchor.x + m.number.x) * size / 2,
                        inset + (m.anchor.y + m.number.y) * size / 2, radius))
                }
                assertNull(hitLug(markers, size, inset, -100f, -100f, radius))
            }
        }
        val pilot = lugMarkers(floor)
        assertTrue(pilot.first().number.y < 0f) // above the top of the photograph
        assertTrue(pilot[2].number.x < pilot[2].anchor.x)
        assertTrue(pilot[5].number.x > pilot[5].anchor.x)
        assertTrue(pilot[3].number.y in .35f.. .50f) // inside the skin, above the front bolt
    }
}
