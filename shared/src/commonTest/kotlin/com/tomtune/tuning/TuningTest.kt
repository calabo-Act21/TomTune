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
    private fun confirmAndNext(c: TuningController, hz: Double) { confirm(c, hz); c.nextLug() }
    private fun toCenter(c: TuningController) {
        c.start()
        repeat(2) { repeat(c.state.value!!.drum.lugs!!) { confirmAndNext(c, c.state.value!!.localTarget) }; c.nextHead() }
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
        val oldStrike = ++strike
        c.acceptEvent(original, StrikeEvent.Detected(oldStrike, 0.0))
        c.selectLug(3)
        c.acceptEvent(original, StrikeEvent.Estimate(oldStrike, .32, 165.0, .9))
        assertEquals(0, c.state.value!!.seriesCount)
        val current = c.captureGeneration
        val rejectedStrike = ++strike
        c.acceptEvent(current, StrikeEvent.Detected(rejectedStrike, 0.0))
        c.acceptEvent(current, StrikeEvent.Rejected(rejectedStrike, .32, RejectionReason.Clipped))
        assertEquals(Feedback.REJECTED, c.state.value!!.feedback)
        assertEquals(0, c.state.value!!.seriesCount)
        confirm(c, 165.0)
        assertEquals(3, c.state.value!!.lug)
        assertTrue(c.state.value!!.measurements.isEmpty())
        c.nextLug()
        assertEquals(4, c.state.value!!.lug)
        assertEquals(mapOf(3 to 165.0), c.state.value!!.measurements)
        repeat(3) { hit(c, 165.0, current) }
        assertEquals(4, c.state.value!!.lug)
        c.selectLug(3); confirmAndNext(c, 166.0)
        assertEquals(166.0, c.state.value!!.measurements[3])
        assertEquals(4, c.state.value!!.lug)
        c.dispose()
    }

    @Test fun debugBlockResetKeepsSameLugAndReferenceButInvalidatesStableDraftAndLateEvents() = runTest {
        val source = Source(); val trace = mutableListOf<String>(); var saves = 0
        val c = TuningController(source, backgroundScope, { _, _ -> saves++; true },
            StandardTestDispatcher(testScheduler), trace::add, diagnosticComparison = true)
        c.open(kit(), drum); c.start(); confirm(c, 491.0)
        assertTrue(c.state.value!!.readyForNext)
        val old = c.captureGeneration
        c.debugResetSeries()
        val s = c.state.value!!
        assertTrue(source.running); assertTrue(c.captureGeneration > old)
        assertEquals(0, s.lug); assertEquals(Head.BATTER, s.head)
        assertNull(s.stableHz); assertNull(s.provisionalHz); assertEquals(0, s.seriesCount)
        assertFalse(s.readyForNext); assertTrue(s.measurements.isEmpty())
        assertEquals(drum.reference, s.drum.reference)
        repeat(3) { hit(c, 699.0, old) }
        c.nextLug(); assertEquals(0, c.state.value!!.lug)
        assertEquals(0, c.state.value!!.seriesCount); assertEquals(0, saves)
        assertTrue(trace.any { "DebugReset reason=operator" in it })
        assertTrue(trace.any { "UI readyForNext=false stableHz=null" in it })
        confirm(c, 491.0); assertTrue(c.state.value!!.readyForNext)
        c.nextLug(); assertEquals(1, c.state.value!!.lug)
        c.dispose()
    }

    @Test fun stableDraftRefinesThenContradictionReconfirmsWithoutMovingAndRejectedStrikesDoNotCount() = runTest {
        val source = Source(); val trace = mutableListOf<String>()
        val c = TuningController(source, backgroundScope, { _, _ -> true }, StandardTestDispatcher(testScheduler), trace::add)
        c.open(kit(), drum); c.start()
        c.nextLug(); assertEquals(0, c.state.value!!.lug)
        hit(c, 350.0); assertEquals(350.0, c.state.value!!.provisionalHz)
        assertFalse(c.state.value!!.readyForNext)
        hit(c, 350.1); hit(c, 350.2)
        assertEquals(350.1, c.state.value!!.stableHz); assertTrue(c.state.value!!.readyForNext)
        val generation = c.captureGeneration
        assertTrue(source.running); assertTrue(c.state.value!!.measurements.isEmpty())
        hit(c, 350.3); assertEquals(350.2, c.state.value!!.stableHz)
        val id = ++strike
        c.acceptEvent(generation, StrikeEvent.Detected(id, 0.0))
        assertEquals(350.3, c.state.value!!.provisionalHz) // keep the last reading while analyzing the next attack
        c.acceptEvent(generation, StrikeEvent.Rejected(id, .32, RejectionReason.Noise))
        assertEquals(4, c.state.value!!.seriesCount); assertTrue(c.state.value!!.readyForNext)
        hit(c, 700.0)
        assertEquals(700.0, c.state.value!!.provisionalHz)
        assertEquals(350.2, c.state.value!!.stableHz)
        assertFalse(c.state.value!!.readyForNext); assertEquals(1, c.state.value!!.seriesCount)
        c.nextLug(); assertEquals(0, c.state.value!!.lug)
        hit(c, 700.1); hit(c, 700.2)
        assertEquals(700.1, c.state.value!!.stableHz); assertTrue(c.state.value!!.readyForNext)
        assertEquals(generation, c.captureGeneration); assertTrue(source.running)
        assertTrue(c.state.value!!.measurements.isEmpty()); c.nextLug()
        assertEquals(1, c.state.value!!.lug); assertEquals(mapOf(0 to 700.1), c.state.value!!.measurements)
        c.nextLug(); assertEquals(1, c.state.value!!.lug)
        assertTrue(trace.any { "SeriesReset reason=incompatible" in it })
        assertTrue(trace.any { "Rejected" in it && "reason=Noise" in it })
        assertTrue(trace.any { "Confirmed" in it && "hz=700.1" in it })
        c.dispose()
    }

    @Test fun interruptionAndPcmGapRequireExplicitResumeWithEmptySeries() = runTest {
        val source = Source(); val c = controller(source); c.open(kit(), drum); c.start(); confirmAndNext(c, 165.0)
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
        c.resume(); confirmAndNext(c, 165.0); assertEquals(2, c.state.value!!.lug)
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
        repeat(2) { repeat(6) { confirmAndNext(c, c.state.value!!.localTarget) }; c.nextHead() }
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
        repeat(6) { confirmAndNext(c, 180.0) }; c.repeatHead()
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

    @Test fun tenWeakInjectedStrikesThenStrongerOctaveOnSameLugWithTargetIndependence() = runTest {
        for (midi in listOf(28, 60)) {
            val source = Source(); val trace = mutableListOf<String>()
            val c = TuningController(source, backgroundScope, { _, _ -> true }, StandardTestDispatcher(testScheduler), trace::add)
            c.open(kit(), drum); c.chooseMidi(midi); c.start()
            suspend fun pcm(hz: Double, amplitude: Double, secondAmplitude: Double = 0.0) {
                repeat(200) { block ->
                    val samples = FloatArray(80) { i ->
                        val t = (block * 80 + i) / 8000.0 - .2
                        if (t < 0) 0f else (exp(-7 * t) * (amplitude * sin(2 * PI * hz * t) +
                            secondAmplitude * sin(2 * PI * 700 * t))).toFloat()
                    }
                    source.sink.onPcm(PcmBlock(samples, 8000, source.frame)); source.frame += 80; runCurrent()
                }
            }
            repeat(10) { index ->
                pcm(350.0, .025)
                assertEquals(0, c.state.value!!.lug); assertTrue(source.running)
                assertEquals(index >= 2, c.state.value!!.readyForNext)
            }
            assertEquals(10, trace.count { " Detected " in it })
            assertEquals(0, trace.count { " Rejected " in it })
            assertEquals(350.0, c.state.value!!.stableHz!!, .5)
            assertTrue(trace.first { " Confirmed " in it }.contains("strike=3"))
            val oldStable = c.state.value!!.stableHz
            pcm(350.0, .25, .35) // two persistent octave modes: detected but inconclusive
            assertEquals(Feedback.REJECTED, c.state.value!!.feedback)
            assertEquals(RejectionReason.AmbiguousModes, c.state.value!!.rejection)
            assertEquals(5, c.state.value!!.seriesCount); assertEquals(oldStable, c.state.value!!.stableHz)
            pcm(700.0, .35)
            assertEquals(700.0, c.state.value!!.provisionalHz!!, .5)
            assertEquals(oldStable, c.state.value!!.stableHz); assertFalse(c.state.value!!.readyForNext)
            c.nextLug(); assertEquals(0, c.state.value!!.lug)
            pcm(700.0, .35); pcm(700.0, .35)
            assertEquals(700.0, c.state.value!!.stableHz!!, .5)
            assertTrue(c.state.value!!.readyForNext); assertTrue(source.running)
            println("PCM lug targetMidi=$midi: ten weak=10 detected/0 rejected; stable on #3; ambiguous350700 rejected; stronger700 stable after 3; no advancement before Next")
            c.nextLug(); assertEquals(1, c.state.value!!.lug)
            assertEquals(700.0, c.state.value!!.measurements.getValue(0), .5)
            c.dispose()
        }
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
                assertEquals(lug, c.state.value!!.lug)
                assertTrue(source.running)
                assertTrue(c.state.value!!.readyForNext)
                c.nextLug()
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
