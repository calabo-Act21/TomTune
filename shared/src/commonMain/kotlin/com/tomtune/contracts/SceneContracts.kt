package com.tomtune.contracts

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class LugPosition(val index: Int, val x: Double, val y: Double)
/** Canonical screen coordinates: right +x, down +y, noon -PI/2. */
object LugLayout {
    fun positions(count: Int): List<LugPosition> {
        require(count >= 3)
        return List(count) { index ->
            val angle = -PI / 2 + 2 * PI * (index + 0.5) / count
            LugPosition(index, cos(angle), sin(angle))
        }
    }
}
data class TomSceneState(val lugCount: Int = 8, val selectedLug: Int = 0) {
    init { require(lugCount >= 3 && selectedLug in 0 until lugCount) }
}
interface TomSelectionSink { fun selectLug(index: Int) }
