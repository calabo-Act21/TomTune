package com.tomtune.tuning

import com.tomtune.contracts.*
import com.tomtune.kit.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TuningTest {
    private class Source : PcmSource {
        lateinit var sink: PcmSink
        var frame = 0L
        var running = false
        override fun start(sink: PcmSink) { this.sink = sink; frame = 0; running = true }
        override fun stop() { running = false }
    }
    private val drum = Drum("tom", DrumFamily.RACK, 0, 12, 6,
        reference = DrumReference(110.0, TargetOrigin.CAPTURED, 1))
    private fun kit(d: Drum = drum) = Kit("kit", "Studio", listOf(d,
        Drum("snare", DrumFamily.SNARE, 1, 14), Drum("kick", DrumFamily.KICK, 2, 22)))
    private class Repo(var disk: KitStore) : KitRepository {
        var fail = false
        var writes = 0
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun load() = disk
        override suspend fun write(store: KitStore) { writes++; gate?.await(); if (fail) error("full"); disk = store }
    }
    private fun TestScope.controller(source: Source, save: suspend (TuningState, Boolean) -> Boolean = { _, _ -> true }) =
        TuningController(source, backgroundScope, save, StandardTestDispatcher(testScheduler))
    private var strike = 0L
    private fun hit(c: TuningController, hz: Double, gen: Long = c.captureGeneration): Long {
        val id = ++strike
        c.acceptEvent(gen, StrikeEvent.Detected(id, 0.0))
        c.acceptEvent(gen, StrikeEvent.Estimate(id, .32, hz, .9))
        return id
    }
    private fun confirm(c: TuningController, hz: Double) { repeat(3) { hit(c, hz) } }
    private fun toCenter(c: TuningController) {
        c.start()
        repeat(2) { repeat(c.state.value!!.drum.lugs!!) { confirm(c, c.state.value!!.localTarget) }; c.nextHead() }
        assertEquals(TuningStage.CENTER, c.state.value!!.stage)
    }

    @Test fun diagramHasTheRealLugCountClockwiseWithLogoBetweenTheTopPair() {
        for (count in 3..12) {
            val points = schematicLugAnchors(count)
            assertEquals(count, points.size); assertEquals(count, points.distinct().size)
            assertTrue(points.all { it.x in .14f.. .86f && it.y in .14f.. .86f })
            assertTrue(points.first().x > .5f && points.last().x < .5f)
            assertEquals(points.first().y, points.last().y, .00001f)
        }
        assertEquals(Advice.LEAVE, advice(100.0, 100.0))
        assertEquals(Advice.TIGHTEN, advice(99.0, 100.0))
        assertEquals(Advice.LOOSEN, advice(101.0, 100.0))
    }

    @Test fun quickLongIncoherentSeriesAndMedian() {
        val s = StrikeSeries()
        val f = { cents: Double -> 100 * 2.0.pow(cents / 1200) }
        assertNull(s.add(f(0.0))); assertNull(s.add(f(3.0))); assertEquals(f(1.0), s.add(f(1.0)))
        s.reset()
        listOf(0.0, 15.0, 5.0, 13.0).forEach { assertNull(s.add(f(it))) }
        assertEquals(f(8.0), s.add(f(8.0)))
        s.reset(); s.add(100.0); assertNull(s.add(120.0)); assertTrue(s.unstable); assertEquals(1, s.count)
        assertNull(s.add(120.0)); assertEquals(120.0, s.add(120.0))
    }

    @Test fun duplicatesLateEventsManualSelectionAndRejectionsNeverSkipALug() = runTest {
        val c = controller(Source()); c.open(kit(), drum); c.start()
        val original = c.captureGeneration
        val id = hit(c, 165.0)
        repeat(10) { c.acceptEvent(original, StrikeEvent.Estimate(id, .32, 165.0, .9)) }
        assertEquals(1, c.state.value!!.seriesCount)
        c.acceptEvent(original, StrikeEvent.Estimate(999, .32, 165.0, .9)) // no detection
        c.acceptEvent(original, StrikeEvent.Detected(10_000, 0.0))
        c.selectLug(3)
        c.acceptEvent(original, StrikeEvent.Estimate(10_000, .32, 165.0, .9))
        assertEquals(0, c.state.value!!.seriesCount)
        val current = c.captureGeneration
        c.acceptEvent(current, StrikeEvent.Detected(10_001, 0.0))
        c.acceptEvent(current, StrikeEvent.Rejected(10_001, .32, RejectionReason.Clipped))
        assertEquals(Feedback.REJECTED, c.state.value!!.feedback)
        assertEquals(0, c.state.value!!.seriesCount)
        confirm(c, 165.0)
        assertEquals(4, c.state.value!!.lug)
        assertEquals(mapOf(3 to 165.0), c.state.value!!.measurements)
        repeat(3) { hit(c, 165.0, current) }
        assertEquals(4, c.state.value!!.lug)
        c.selectLug(3); confirm(c, 166.0)
        assertEquals(166.0, c.state.value!!.measurements[3])
        assertEquals(4, c.state.value!!.lug)
        c.dispose()
    }

    @Test fun interruptionAndPcmGapRequireExplicitResumeWithEmptySeries() = runTest {
        val source = Source(); val c = controller(source); c.open(kit(), drum); c.start(); confirm(c, 165.0)
        hit(c, 165.0); val old = source.sink; val gen = c.captureGeneration
        c.interrupt(); assertFalse(source.running); assertEquals(1, c.state.value!!.lug)
        assertEquals(mapOf(0 to 165.0), c.state.value!!.measurements)
        c.resume(); assertEquals(0, c.state.value!!.seriesCount)
        old.onIssue(CaptureIssue.PermissionDenied); hit(c, 165.0, gen); runCurrent()
        assertTrue(c.state.value!!.listening)
        source.sink.onPcm(PcmBlock(FloatArray(480), 48000, 0)); runCurrent()
        source.sink.onPcm(PcmBlock(FloatArray(480), 48000, 960)); runCurrent()
        assertEquals(CaptureIssue.Overrun, c.state.value!!.issue)
        assertFalse(source.running); assertEquals(0, c.state.value!!.seriesCount)
        c.resume(); confirm(c, 165.0); assertEquals(2, c.state.value!!.lug)
        c.dispose()
    }

    @Test fun synchronousPermissionFailureStaleRepliesAndAllCaptureIssuesCanResume() = runTest {
        val source = Source()
        val c = controller(source); c.open(kit(), drum); c.start()
        hit(c, 165.0); hit(c, 180.0)
        assertEquals(Feedback.UNSTABLE, c.state.value!!.feedback)
        assertEquals(1, c.state.value!!.seriesCount)
        for (issue in CaptureIssue.entries) {
            val old = source.sink
            source.sink.onIssue(issue); runCurrent()
            assertFalse(c.state.value!!.listening); assertEquals(issue, c.state.value!!.issue)
            assertEquals(0, c.state.value!!.seriesCount); assertFalse(source.running)
            c.resume(); runCurrent()
            old.onIssue(CaptureIssue.Unavailable); runCurrent()
            assertTrue(c.state.value!!.listening); assertNull(c.state.value!!.issue)
        }
        c.dispose()
        val denied = object : PcmSource {
            override fun start(sink: PcmSink) { sink.onIssue(CaptureIssue.PermissionDenied) }
            override fun stop() {}
        }
        val sync = TuningController(denied, backgroundScope, { _, _ -> false }, StandardTestDispatcher(testScheduler))
        sync.open(kit(), drum); sync.start(); runCurrent()
        assertEquals(CaptureIssue.PermissionDenied, sync.state.value!!.issue)
        assertFalse(sync.state.value!!.listening)
        sync.dispose()
    }

    @Test fun repeatCorrectionAbandonAndBothFinishActionsPreserveTheRightData() = runTest {
        val repo = Repo(KitStore(kits = listOf(kit()), activeId = "kit"))
        val kits = KitController(repo, KitRules({ "new" }, VisualCatalogue(emptyList(), emptyMap())), backgroundScope)
        runCurrent(); kits.openDrum(drum.id)
        val c = controller(Source()) { session, reference -> kits.finishTuning(session, reference, 100) }
        c.open(kit(), drum); c.chooseMidi(47); assertEquals(123.47082531403103, c.state.value!!.targetHz, 1e-9)
        toCenter(c); confirm(c, 120.0)
        assertEquals(-49, cents(120.0, c.state.value!!.targetHz).roundToInt())
        assertEquals(151, cents(120.0, drum.reference!!.hz).roundToInt())
        c.correct(); assertNull(c.state.value!!.centerHz); c.finish(false); runCurrent(); assertEquals(0, repo.writes)
        repeat(2) { repeat(6) { confirm(c, c.state.value!!.localTarget) }; c.nextHead() }
        confirm(c, 120.0)
        repo.fail = true; c.finish(false); runCurrent()
        assertEquals(120.0, c.state.value!!.centerHz); assertTrue(c.state.value!!.writeFailed)
        assertNull(repo.disk.active!!.drums.first().lastMeasurement)
        repo.fail = false; repo.gate = CompletableDeferred()
        c.finish(false); c.finish(true); runCurrent(); assertEquals(2, repo.writes)
        repo.gate!!.complete(Unit); runCurrent(); assertNull(c.state.value)
        assertEquals(110.0, repo.disk.active!!.drums.first().reference!!.hz)
        assertEquals(120.0, repo.disk.active!!.drums.first().lastMeasurement!!.hz)
        assertEquals(KitScreen.DRUM, kits.state.value.screen); assertEquals(drum.id, kits.state.value.selectedDrumId)
        val saved = repo.disk.active!!.drums.first()
        c.open(repo.disk.active!!, saved); c.chooseMidi(47); toCenter(c); confirm(c, 120.0); c.finish(true); runCurrent()
        assertEquals(midiHz(47), repo.disk.active!!.drums.first().reference!!.hz)
        assertEquals(TargetOrigin.MANUAL, repo.disk.active!!.drums.first().reference!!.origin)
        assertEquals(120.0, repo.disk.active!!.drums.first().lastMeasurement!!.hz)
        c.open(repo.disk.active!!, repo.disk.active!!.drums.first()); c.start()
        repeat(6) { confirm(c, 180.0) }; c.repeatHead()
        assertTrue(c.state.value!!.measurements.isEmpty()); assertEquals(2, c.state.value!!.pass)
        val disk = repo.disk; c.requestAbandon(); c.abandon(); runCurrent(); assertNull(c.state.value); assertEquals(disk, repo.disk)
        val reopened = KitController(repo, KitRules({ "x" }, VisualCatalogue(emptyList(), emptyMap())), backgroundScope)
        runCurrent(); assertEquals(repo.disk, reopened.state.value.store)
        c.dispose()
    }

    @Test fun exactReferenceOutsideManualSelectorIsRetainedUntilAnExplicitChoice() = runTest {
        val d = drum.copy(reference = DrumReference(913.271, TargetOrigin.CAPTURED, 1))
        val c = controller(Source()); c.open(kit(d), d)
        assertEquals(913.271, c.state.value!!.targetHz); assertTrue(c.state.value!!.fromReference)
        c.chooseMidi(61); assertEquals(913.271, c.state.value!!.targetHz)
        c.chooseMidi(28); assertEquals(midiHz(28), c.state.value!!.targetHz)
        assertEquals(TargetOrigin.MANUAL, c.state.value!!.targetOrigin)
        assertEquals(913.271, d.reference!!.hz)
        c.start(); c.chooseMidi(60); assertEquals(midiHz(28), c.state.value!!.targetHz)
        c.dispose()
    }

    @Test fun configurationGuardAndDiameterResetPreserveReferencesOnRename() = runTest {
        val rules = KitRules({ "x" }, VisualCatalogue(emptyList(), emptyMap()))
        val measured = drum.copy(lastMeasurement = GlobalMeasurement(120.0, 1))
        val k = kit(measured)
        assertEquals(measured.reference, rules.lugs(k, measured.id, 7).drums.first().reference)
        assertEquals(measured, k.copy(name = "renamed").drums.first())
        val changed = rules.diameter(k, drum.id, 10).drums.first()
        assertNull(changed.reference); assertNull(changed.lastMeasurement)
        val repo = Repo(KitStore(kits = listOf(k), activeId = k.id))
        val controller = KitController(repo, rules, backgroundScope); runCurrent()
        val incompatible = TuningState(1, k.id, measured.copy(lugs = 7), 110.0, TargetOrigin.MANUAL, false, centerHz = 120.0)
        assertFalse(controller.finishTuning(incompatible, true, 2)); assertEquals(0, repo.writes)
        assertFalse(controller.finishTuning(incompatible.copy(drum = measured.copy(id = "deleted")), true, 2))
    }

    /** Full source -> ordered worker -> analyzer -> stability -> both heads -> correction -> disk.
     * These are synthetic decaying PCM strikes, never microphone or acoustic validation. */
    @Test fun completeInjectedPcmJourneyIncludingLowCenterAndFarFromTarget() = runTest {
        val d = drum.copy(family = DrumFamily.FLOOR, diameter = 18, lugs = 3, reference = null)
        val repo = Repo(KitStore(kits = listOf(kit(d)), activeId = "kit"))
        val kits = KitController(repo, KitRules({ "x" }, VisualCatalogue(emptyList(), emptyMap())), backgroundScope)
        runCurrent(); kits.openDrum(d.id)
        val source = Source(); val c = controller(source) { session, reference -> kits.finishTuning(session, reference, 200) }
        c.open(repo.disk.active!!, d); c.start(); val target = c.state.value!!.targetHz
        suspend fun strike(hz: Double) {
            val sink = source.sink
            val rate = 8000
            for (block in 0 until 100) {
                if (source.sink !== sink) break
                val start = source.frame
                val samples = FloatArray(80) { index ->
                    val t = (block * 80 + index) / 8000.0 - .10
                    if (t < 0) 0f else (.35 * exp(-9 * t) * sin(2 * PI * hz * t)).toFloat()
                }
                sink.onPcm(PcmBlock(samples, rate, start)); source.frame += 80; runCurrent()
            }
        }
        suspend fun headPass() {
            val head = c.state.value!!.head
            repeat(3) { lug ->
                repeat(3) { strike(c.state.value!!.localTarget) }
                assertTrue(c.state.value!!.measurements.containsKey(lug), "head=$head lug=$lug ${c.state.value}")
            }
            assertEquals(TuningStage.ADVICE, c.state.value!!.stage); assertFalse(source.running)
        }
        headPass(); c.repeatHead(); headPass(); c.nextHead(); headPass(); c.nextHead()
        repeat(3) { strike(42.0) }
        assertEquals(TuningStage.CENTER, c.state.value!!.stage)
        assertEquals(42.0, c.state.value!!.centerHz!!, .25)
        assertTrue(c.state.value!!.needsCorrection); assertFalse(source.running)
        c.correct(); assertNull(c.state.value!!.centerHz); assertEquals(target, c.state.value!!.targetHz)
        headPass(); c.nextHead(); headPass(); c.nextHead()
        repeat(3) { strike(120.0) } // real frequency far from the fixed ~65 Hz target
        assertEquals(120.0, c.state.value!!.centerHz!!, .25)
        c.finish(true); runCurrent(); assertNull(c.state.value)
        val saved = repo.disk.active!!.drums.first()
        assertEquals(target, saved.reference!!.hz)
        assertEquals(120.0, saved.lastMeasurement!!.hz, .25)
        assertEquals(d.id, kits.state.value.selectedDrumId)
        println("PCM injected complete: floor 18 / 3 lugs, repeat + both heads + 42 Hz center + correction + 120 Hz center; target=$target; saved=${saved.lastMeasurement!!.hz}")
        c.dispose()
    }
}
