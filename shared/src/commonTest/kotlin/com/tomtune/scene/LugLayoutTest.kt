package com.tomtune.scene

import com.tomtune.contracts.LugLayout
import com.tomtune.contracts.TomSceneState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LugLayoutTest {
    @Test fun canonicalIndicesAndSymmetry() {
        for (count in listOf(5, 6, 8, 10)) {
            val positions = LugLayout.positions(count)
            assertEquals(count, positions.size)
            positions.forEachIndexed { index, point ->
                assertEquals(index, point.index)
                assertEquals(1.0, sqrt(point.x * point.x + point.y * point.y), 1e-12)
                val reflected = positions[count - index - 1]
                assertEquals(point.x, -reflected.x, 1e-12)
                assertEquals(point.y, reflected.y, 1e-12)
                assertEquals(index, TomSceneState(count, index).selectedLug)
                val next = positions[(index + 1) % count]
                var step = atan2(next.y, next.x) - atan2(point.y, point.x)
                if (step < 0) step += 2 * PI
                assertEquals(2 * PI / count, step, 1e-12)
            }
            // Noon lies strictly between the two top lugs for odd and even counts.
            assertTrue(positions.first().x > 0 && positions.last().x < 0)
            assertTrue(positions.first().y < 0 && positions.last().y < 0)
            assertTrue(abs(positions.first().y - positions.last().y) < 1e-12)
        }
    }

    @Test fun invalidSelectionCannotReachNativeRenderer() {
        assertFailsWith<IllegalArgumentException> { TomSceneState(8, 8) }
        assertFailsWith<IllegalArgumentException> { TomSceneState(8, -1) }
        assertFailsWith<IllegalArgumentException> { LugLayout.positions(2) }
    }
}
