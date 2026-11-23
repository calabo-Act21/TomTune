package com.tomtune.app

import com.tomtune.session.MeasurementController

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.tomtune.contracts.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.UIKit.UIViewController

interface IOSSceneHost {
    fun makeView(selection: TomSelectionSink): UIViewController
    fun updateView(view: UIViewController, state: TomSceneState)
}
private val LocalSceneHost = staticCompositionLocalOf<IOSSceneHost> { error("Scene host required") }
fun MainViewController(source: PcmSource, sceneHost: IOSSceneHost): UIViewController {
    val controller = MeasurementController(stopCapture = { source.stop() })
    return ComposeUIViewController {
        CompositionLocalProvider(LocalSceneHost provides sceneHost) { App(controller, source::start) }
    }
}
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeTomScene(state: TomSceneState, onSelect: (Int) -> Unit, modifier: Modifier) {
    val host = LocalSceneHost.current
    val callback by rememberUpdatedState(onSelect)
    UIKitViewController(factory = {
        host.makeView(object : TomSelectionSink { override fun selectLug(index: Int) { callback(index) } })
    }, update = { host.updateView(it, state) }, modifier = modifier)
}
@OptIn(ExperimentalForeignApi::class)
actual fun formatHz(value: Double): String = NSNumberFormatter().apply {
    numberStyle = NSNumberFormatterDecimalStyle
    minimumFractionDigits = 1u
    maximumFractionDigits = 1u
}.stringFromNumber(NSNumber(value)) ?: value.toString()
