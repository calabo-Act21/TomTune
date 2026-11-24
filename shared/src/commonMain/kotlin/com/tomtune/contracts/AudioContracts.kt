package com.tomtune.contracts

/** Mono normalized PCM. Samples owned by the recipient; time is relative to capture start. */
data class PcmBlock(val samples: FloatArray, val sampleRate: Int, val startFrame: Long)

enum class CaptureIssue { PermissionDenied, Interrupted, RouteChanged, Unavailable, Overrun }
interface PcmSink {
    /** Never called on the UI/render thread. Blocks are ordered, contiguous within a session. */
    fun onPcm(block: PcmBlock)
    fun onIssue(issue: CaptureIssue)
}
interface PcmSource {
    /** Permission is handled by the platform host before starting. One active session maximum. */
    fun start(sink: PcmSink)
    /** Synchronous invalidation; queued callbacks must not reach the previous sink. */
    fun stop()
}

enum class RejectionReason { AmbiguousModes, Noise, Clipped, TooShort }
sealed interface StrikeEvent {
    val strikeId: Long
    val timeSeconds: Double
    data class Detected(override val strikeId: Long, override val timeSeconds: Double) : StrikeEvent
    data class Estimate(override val strikeId: Long, override val timeSeconds: Double,
                        val frequencyHz: Double, val confidence: Double) : StrikeEvent
    data class Rejected(override val strikeId: Long, override val timeSeconds: Double,
                        val reason: RejectionReason) : StrikeEvent
}
interface StrikeAnalyzer {
    fun accept(block: PcmBlock): List<StrikeEvent>
    /** Flush a pending strike when an injected recording ends. */
    fun finish(): List<StrikeEvent>
    fun reset()
}
