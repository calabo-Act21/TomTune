package com.tomtune.kit

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class KitScreen { LOADING, LOAD_ERROR, WELCOME, CONFIGURE, PREVIEW, HOME, DRUM }
enum class KitError { NAME_EMPTY, NAME_LONG, NAME_DUPLICATE, WRITE }
data class KitState(val store: KitStore = KitStore(), val screen: KitScreen = KitScreen.LOADING,
    val draft: Kit? = null, val editing: Boolean = false, val selectedDrumId: String? = null,
    val busy: Boolean = false, val error: KitError? = null)

/** UI mutations run on the caller's UI scope; repositories move disk IO off that dispatcher. */
class KitController(private val repository: KitRepository, private val rules: KitRules, private val scope: CoroutineScope) {
    private val mutable = MutableStateFlow(KitState())
    val state = mutable.asStateFlow()
    init { load() }
    fun load() {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(screen = KitScreen.LOADING, busy = true)
        scope.launch {
            try {
                val loaded = repository.load().validated()
                mutable.value = KitState(store = loaded, screen = if (loaded.active == null) KitScreen.WELCOME else KitScreen.HOME)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = mutable.value.copy(screen = KitScreen.LOAD_ERROR, busy = false) }
        }
    }
    fun configure(edit: Boolean = false) {
        val s = mutable.value
        if (s.busy) return
        val kit = if (edit) s.store.active ?: return else rules.fresh()
        mutable.value = s.copy(screen = KitScreen.CONFIGURE, draft = kit.copy(drums = kit.drums.toList()), editing = edit, error = null)
    }
    fun change(transform: (Kit) -> Kit) {
        val s = mutable.value
        if (!s.busy && s.draft != null) mutable.value = s.copy(draft = transform(s.draft), error = null)
    }
    fun count(family: DrumFamily, count: Int) = change { rules.count(it, family, count) }
    fun diameter(id: String, diameter: Int) = change { rules.diameter(it, id, diameter) }
    fun lugs(id: String, count: Int) = change { rules.lugs(it, id, count) }
    fun name(name: String) = change { it.copy(name = name) }
    fun preview() { if (!mutable.value.busy && mutable.value.draft != null) mutable.value = mutable.value.copy(screen = KitScreen.PREVIEW, error = null) }
    fun back() {
        val s = mutable.value
        if (s.busy) return
        mutable.value = when (s.screen) {
            KitScreen.PREVIEW -> s.copy(screen = KitScreen.CONFIGURE, error = null)
            KitScreen.CONFIGURE -> s.copy(screen = if (s.store.active == null) KitScreen.WELCOME else KitScreen.HOME, draft = null, error = null)
            KitScreen.DRUM -> s.copy(screen = KitScreen.HOME, selectedDrumId = null)
            else -> s
        }
    }
    fun openDrum(id: String) {
        val s = mutable.value
        if (!s.busy && s.store.active?.drums?.any { it.id == id } == true)
            mutable.value = s.copy(screen = KitScreen.DRUM, selectedDrumId = id)
    }
    fun save() {
        val s = mutable.value
        if (s.busy || s.screen != KitScreen.PREVIEW) return
        val draft = s.draft ?: return
        val name = draft.name.trim()
        val error = when {
            name.isEmpty() -> KitError.NAME_EMPTY
            name.length > 60 -> KitError.NAME_LONG
            s.store.kits.any { it.id != draft.id && it.name.equals(name, ignoreCase = true) } -> KitError.NAME_DUPLICATE
            else -> null
        }
        if (error != null) { mutable.value = s.copy(error = error); return }
        val saved = draft.copy(name = name, revision = draft.revision + 1)
        val kits = if (s.editing) s.store.kits.map { if (it.id == saved.id) saved else it } else s.store.kits + saved
        commit(s.store.copy(kits = kits, activeId = saved.id))
    }
    fun select(id: String) {
        val s = mutable.value
        if (s.busy || s.store.activeId == id || s.store.kits.none { it.id == id }) return
        commit(s.store.copy(activeId = id))
    }
    /** Commit both optional fields in the existing atomic snapshot, addressed by stable IDs. */
    suspend fun finishTuning(session: com.tomtune.tuning.TuningState, withReference: Boolean, now: Long): Boolean {
        val s = mutable.value
        if (s.busy || session.centerHz == null) return false
        val kit = s.store.kits.find { it.id == session.kitId } ?: return false
        val drum = kit.drums.find { it.id == session.drum.id } ?: return false
        if (drum.family != session.drum.family || drum.diameter != session.drum.diameter || drum.lugs != session.drum.lugs) return false
        val updated = drum.copy(lastMeasurement = GlobalMeasurement(session.centerHz, now),
            reference = if (withReference) DrumReference(session.targetHz, session.targetOrigin, now) else drum.reference)
        val nextKit = kit.copy(drums = kit.drums.map { if (it.id == drum.id) updated else it }, revision = kit.revision + 1)
        val next = s.store.copy(kits = s.store.kits.map { if (it.id == kit.id) nextKit else it })
        mutable.value = s.copy(busy = true, error = null)
        return try {
            repository.write(next.validated())
            mutable.value = s.copy(store = next, screen = KitScreen.DRUM, selectedDrumId = drum.id, busy = false)
            true
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.value = s.copy(busy = false); false }
    }
    private fun commit(next: KitStore) {
        val previous = mutable.value
        mutable.value = previous.copy(busy = true, error = null)
        scope.launch {
            try {
                repository.write(next.validated())
                mutable.value = KitState(store = next, screen = KitScreen.HOME)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = previous.copy(busy = false, error = KitError.WRITE) }
        }
    }
}
