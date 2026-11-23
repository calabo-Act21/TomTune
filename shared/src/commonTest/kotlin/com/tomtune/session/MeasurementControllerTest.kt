package com.tomtune.session

import com.tomtune.contracts.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.math.*

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementControllerTest {
    @Test fun stoppingDiscardsQueuedPcmAndLateCallbacks() = runTest {
        var releases = 0
        val controller = MeasurementController({ releases++ }, StandardTestDispatcher(testScheduler))
        lateinit var oldSink: PcmSink
        controller.start { oldSink = it }
        oldSink.onPcm(attack())
        controller.stop()
        val stopped = controller.state.value
        oldSink.onPcm(attack())
        oldSink.onIssue(CaptureIssue.Interrupted)
        runCurrent()
        assertEquals(stopped, controller.state.value)
        assertFalse(controller.state.value.listening)
        assertTrue(releases >= 2)
        controller.dispose()
    }
    @Test fun resumeAcceptsFreshStrikeAndIgnoresOldSessionFault() = runTest {
        val controller = MeasurementController({}, StandardTestDispatcher(testScheduler))
        lateinit var oldSink: PcmSink
        lateinit var freshSink: PcmSink
        controller.start { oldSink = it }
        controller.start { freshSink = it }
        oldSink.onIssue(CaptureIssue.PermissionDenied)
        freshSink.onPcm(attack())
        runCurrent()
        assertTrue(controller.state.value.event is StrikeEvent.Detected)
        assertTrue(controller.state.value.listening)
        assertNull(controller.state.value.issue)
        controller.dispose()
    }
    @Test fun overrunIsExplicitAndDoesNotPublishStaleFrequency() = runTest {
        val controller = MeasurementController({}, StandardTestDispatcher(testScheduler))
        lateinit var sink: PcmSink
        controller.start { sink = it }
        repeat(33) { sink.onPcm(PcmBlock(FloatArray(480), 48000, it * 480L)) }
        runCurrent()
        assertEquals(CaptureIssue.Overrun, controller.state.value.issue)
        assertFalse(controller.state.value.listening)
        assertNull(controller.state.value.event)
        controller.dispose()
    }
    @Test fun backgroundAlwaysReleasesCaptureEvenAfterOverrun() = runTest {
        var releases = 0
        val controller = MeasurementController({ releases++ }, StandardTestDispatcher(testScheduler))
        lateinit var sink: PcmSink
        controller.start { sink = it }
        repeat(33) { sink.onPcm(PcmBlock(FloatArray(480), 48000, it * 480L)) }
        assertEquals(CaptureIssue.Overrun, controller.state.value.issue)
        val before = releases
        controller.interrupt()
        assertEquals(before + 1, releases)
        assertEquals(CaptureIssue.Overrun, controller.state.value.issue)
        runCurrent()
        controller.dispose()
    }
    private fun attack() = PcmBlock(FloatArray(12000) { i ->
        val t = i / 48000.0 - .1
        if (t < 0) 0f else (.3 * exp(-7 * t) * sin(2 * PI * 185 * t)).toFloat()
    }, 48000, 0)
}
