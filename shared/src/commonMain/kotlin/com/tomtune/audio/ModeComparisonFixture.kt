package com.tomtune.audio

import kotlin.math.*

/** Synthetic diagnostic only. Amplitude is normalized PCM, never physical strike force. */
object ModeComparisonFixture {
    val frequencies = listOf(491.0, 699.0, 801.0)
    data class Stroke(val amplitudes: List<Double>, val decays: List<Double>)
    private val shapes = listOf(
        Stroke(listOf(.22, .03, .02), listOf(7.0, 7.0, 7.0)),
        Stroke(listOf(.03, .22, .02), listOf(7.0, 7.0, 7.0)),
        Stroke(listOf(.02, .03, .22), listOf(7.0, 7.0, 7.0)),
        Stroke(listOf(.22, .20, .18), listOf(7.0, 7.0, 7.0)),
        Stroke(listOf(.24, .20, .18), listOf(7.0, 18.0, 22.0)),
        Stroke(listOf(.08, .30, .20), listOf(7.0, 18.0, 22.0)),
        Stroke(listOf(.02, .18, .25), listOf(18.0, 7.0, 22.0)),
        Stroke(listOf(.25, .18, .02), listOf(22.0, 7.0, 22.0)),
        Stroke(listOf(.18, .02, .25), listOf(22.0, 22.0, 7.0)),
        Stroke(listOf(.20, .20, .20), listOf(7.0, 7.0, 7.0)),
    )
    fun stroke(index: Int): Stroke {
        val shape = shapes[index % 10]
        val scale = if (index % 20 < 10) .11 else .70
        return shape.copy(amplitudes = shape.amplitudes.map { it * scale })
    }
    fun block(index: Int, startInStroke: Int, size: Int, rate: Int): FloatArray {
        val stroke = stroke(index)
        return FloatArray(size) { i ->
            val t = (startInStroke + i).toDouble() / rate - .20
            if (t < 0) 0f else frequencies.indices.sumOf { mode ->
                stroke.amplitudes[mode] * exp(-stroke.decays[mode] * t) * sin(2 * PI * frequencies[mode] * t)
            }.toFloat()
        }
    }
}
