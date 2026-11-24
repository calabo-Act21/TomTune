package com.tomtune.scene

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.SurfaceView
import android.widget.FrameLayout
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import com.tomtune.contracts.TomSceneState
import java.nio.ByteBuffer

/** All Filament calls run on the Android main thread; audio processing owns a separate worker. */
@SuppressLint("ClickableViewAccessibility")
class FilamentTomView(context: Context, private val onSelect: (Int) -> Unit) : FrameLayout(context) {
    private val surface = SurfaceView(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val choreographer = Choreographer.getInstance()
    private var state = TomSceneState()
    private var closed = false
    private var generation = 0
    private var running = false
    private var downX = 0f
    private var downY = 0f
    private val viewer: ModelViewer
    private val ambient: IndirectLight
    private val backdrop: Skybox
    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (closed || !running) return
            applySelection()
            viewer.render(frameTimeNanos)
            choreographer.postFrameCallback(this)
        }
    }

    init {
        Utils.init()
        // A fixed camera keeps numbering and the noon logo stable during measurement.
        val manipulator = Manipulator.Builder()
            .targetPosition(0f, -.18f, 0f)
            .orbitHomePosition(0f, 5.6f, 3.2f)
            .upVector(0f, 1f, 0f)
            .build(Manipulator.Mode.ORBIT)
        // Registered before ModelViewer's listener: free our additions before it destroys its engine.
        surface.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) { releaseOwnedResources() }
        })
        viewer = ModelViewer(surface, manipulator = manipulator)
        // A longer lens avoids the near rim hiding the shell in a close, almost vertical view.
        viewer.cameraFocalLength = 56f
        ambient = IndirectLight.Builder()
            .irradiance(1, floatArrayOf(.8f, .77f, .72f))
            .intensity(30_000f).build(viewer.engine)
        backdrop = Skybox.Builder().color(.84f, .82f, .78f, 1f).build(viewer.engine)
        viewer.scene.indirectLight = ambient
        viewer.scene.skybox = backdrop
        viewer.engine.lightManager.setDirection(
            viewer.engine.lightManager.getInstance(viewer.light), -.5f, -1f, -.3f
        )
        addView(surface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        surface.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (kotlin.math.abs(event.x - downX) < 20 * resources.displayMetrics.density &&
                        kotlin.math.abs(event.y - downY) < 20 * resources.displayMetrics.density) {
                        val requestGeneration = generation
                        // Filament picking uses bottom-left viewport coordinates and true visible geometry.
                        viewer.view.pick(event.x.toInt(), surface.height - 1 - event.y.toInt(), mainHandler) { result ->
                            if (!closed && requestGeneration == generation) {
                                val name = viewer.asset?.getName(result.renderable).orEmpty()
                                val index = name.substringAfter("lug_", "").toIntOrNull()
                                    ?: name.substringAfter("rod_", "").toIntOrNull()
                                    ?: name.substringAfter("selection_", "").toIntOrNull()
                                if (index != null && index in 0 until state.lugCount) onSelect(index)
                            }
                        }
                        surface.performClick()
                    }
                    true
                }
                else -> true
            }
        }
        loadModel()
    }

    fun update(state: TomSceneState) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (closed) return
        val changedCount = this.state.lugCount != state.lugCount
        this.state = state
        if (changedCount) loadModel() else applySelection()
    }

    private fun loadModel() {
        require(state.lugCount in listOf(5, 6, 8, 10)) { "Lot 0 models: 5, 6, 8 or 10 lugs" }
        generation++
        val data = context.assets.open("tom/tom-${state.lugCount}.glb").use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(data.size).apply { put(data); rewind() }
        viewer.loadModelGlb(buffer)
        applySelection()
    }

    private fun applySelection() {
        val asset = viewer.asset ?: return
        val renderables = viewer.engine.renderableManager
        for (entity in asset.entities) {
            val name = asset.getName(entity).orEmpty()
            if (!name.startsWith("selection_")) continue
            val instance = renderables.getInstance(entity)
            if (instance != 0) {
                val visible = name == "selection_${state.selectedLug}"
                // Layer masking survives the asynchronous glTF loader adding renderables to the scene.
                renderables.setLayerMask(instance, 0xff, if (visible) 0xff else 0)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!closed && !running) {
            running = true
            choreographer.postFrameCallback(frame)
        }
    }

    override fun onDetachedFromWindow() {
        close()
        super.onDetachedFromWindow()
    }

    /** ModelViewer frees its engine, glTF resources and swapchain when its SurfaceView detaches. */
    fun close() {
        if (closed) return
        releaseOwnedResources()
        removeView(surface)
    }

    private fun releaseOwnedResources() {
        if (closed) return
        closed = true
        generation++
        running = false
        choreographer.removeFrameCallback(frame)
        viewer.scene.indirectLight = null
        viewer.scene.skybox = null
        viewer.engine.destroyIndirectLight(ambient)
        viewer.engine.destroySkybox(backdrop)
    }
}
