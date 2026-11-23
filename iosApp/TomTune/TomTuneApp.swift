import SwiftUI
import UIKit
import AVFoundation
import Shared

private final class SceneHost: NSObject, IOSSceneHost {
    func makeView(selection: TomSelectionSink) -> UIViewController {
        let view = TomSceneViewController()
        view.onSelect = { index in selection.selectLug(index: Int32(index)) }
        return view
    }
    func updateView(view: UIViewController, state: TomSceneState) {
        (view as? TomSceneViewController)?.setState(lugCount: Int(state.lugCount), selectedLug: Int(state.selectedLug))
    }
}
/// Permission belongs to the host. Invalidate pending permission replies on stop/background.
private final class PermissionPcmSource: NSObject, PcmSource {
    private let capture = IOSPcmSource()
    private var generation: UInt64 = 0
    func start(sink: PcmSink) {
        stop()
        generation &+= 1
        let token = generation
        AVAudioSession.sharedInstance().requestRecordPermission { [weak self] granted in
            DispatchQueue.main.async {
                guard let self, self.generation == token else { return }
                guard UIApplication.shared.applicationState == .active else {
                    sink.onIssue(issue: .interrupted)
                    return
                }
                if granted { self.capture.start(sink: sink) }
                else { sink.onIssue(issue: .permissionDenied) }
            }
        }
    }
    func stop() { generation &+= 1; capture.stop() }
}
private struct FeasibilityView: UIViewControllerRepresentable {
    func makeCoordinator() -> PermissionPcmSource { PermissionPcmSource() }
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(source: context.coordinator, sceneHost: SceneHost())
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
    static func dismantleUIViewController(_ uiViewController: UIViewController, coordinator: PermissionPcmSource) {
        coordinator.stop()
    }
}
@main
struct TomTuneApp: App {
    var body: some Scene { WindowGroup { FeasibilityView().ignoresSafeArea() } }
}
