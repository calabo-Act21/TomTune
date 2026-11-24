package com.tomtune.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tomtune.kit.*
import kotlinx.coroutines.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent))
        val model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = KitModel(applicationContext) as T
        })[KitModel::class.java]
        setContent {
            val state by model.controller.state.collectAsState()
            BackHandler(enabled = state.screen in listOf(KitScreen.CONFIGURE, KitScreen.PREVIEW, KitScreen.DRUM) || state.busy) {
                model.controller.back()
            }
            KitApp(model.controller, model.catalogue,
                kitVisual = { kit, modifier, onOpen -> AndroidKitVisual(kit, model.catalogue, modifier, onOpen) },
                focusVisual = { drum, modifier -> AndroidFocusVisual(drum, model.catalogue, modifier) })
        }
    }
}

private class KitModel(context: android.content.Context) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val catalogue = readVisualCatalogue(context)
    val controller = KitController(AndroidKitRepository(context), KitRules({ UUID.randomUUID().toString() }, catalogue), scope)
    override fun onCleared() { scope.cancel() }
}
