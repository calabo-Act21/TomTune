import AVFoundation
import UIKit
import Shared

/// AVAudioEngine owns capture; only a copied mono buffer leaves its tap.
/// Kotlin analysis is dispatched by the shared screen, away from UIKit/RealityKit.
// Kotlin/Native ObjC export lowercases PascalCase enum entries (permissiondenied, routechanged).
final class IOSPcmSource: NSObject, PcmSource {
    private let lock = NSRecursiveLock()
    private let queue = DispatchQueue(label: "com.tomtune.pcm", qos: .userInitiated)
    private let queueKey = DispatchSpecificKey<Bool>()
    private var generation: UInt64 = 0
    private var engine: AVAudioEngine?
    private var sink: PcmSink?
    private var observers: [NSObjectProtocol] = []
    private var position: Int64 = 0
    private var outstanding = 0

    override init() {
        super.init()
        queue.setSpecific(key: queueKey, value: true)
    }

    func start(sink: PcmSink) {
        stop()
        lock.lock()
        generation &+= 1
        let token = generation
        self.sink = sink
        lock.unlock()
        queue.async { [weak self] in self?.open(token: token) }
    }

    private func open(token: UInt64) {
        lock.lock()
        guard generation == token else { lock.unlock(); return }
        lock.unlock()
        let session = AVAudioSession.sharedInstance()
        guard session.recordPermission == .granted else {
            fail(token: token, issue: .permissiondenied)
            return
        }
        do {
            try session.setCategory(.record, mode: .measurement, options: [])
            try session.setPreferredIOBufferDuration(0.01)
            try session.setActive(true)
            let capture = AVAudioEngine()
            let input = capture.inputNode
            let format = input.outputFormat(forBus: 0)
            guard format.sampleRate >= 8000, format.channelCount > 0 else {
                fail(token: token, issue: .unavailable)
                return
            }
            position = 0
            outstanding = 0
            engine = capture
            input.installTap(onBus: 0, bufferSize: 480, format: format) { [weak self] buffer, _ in
                guard let self, let channels = buffer.floatChannelData else { return }
                // Copy before returning the engine-owned buffer. Bounded queue avoids retaining
                // an unbounded recording when the analyzer cannot keep up.
                let count = Int(buffer.frameLength)
                let channelCount = Int(buffer.format.channelCount)
                var mono = [Float](repeating: 0, count: count)
                for c in 0..<channelCount {
                    for i in 0..<count { mono[i] += channels[c][i] / Float(channelCount) }
                }
                self.lock.lock()
                guard self.generation == token else { self.lock.unlock(); return }
                if self.outstanding >= 32 {
                    self.lock.unlock()
                    self.queue.async { self.fail(token: token, issue: .overrun) }
                    return
                }
                let frame = self.position
                self.position += Int64(count)
                self.outstanding += 1
                self.lock.unlock()
                let rate = Int32(buffer.format.sampleRate.rounded())
                self.queue.async { [weak self] in
                    guard let self else { return }
                    self.lock.lock()
                    defer { self.lock.unlock() }
                    guard self.generation == token else { return }
                    self.outstanding -= 1
                    let samples = KotlinFloatArray(size: Int32(mono.count))
                    for (i, value) in mono.enumerated() { samples.set(index: Int32(i), value: value) }
                    self.sink?.onPcm(block: PcmBlock(samples: samples, sampleRate: rate, startFrame: frame))
                }
            }
            let center = NotificationCenter.default
            observers = [
                center.addObserver(forName: AVAudioSession.interruptionNotification, object: session, queue: nil) { [weak self] note in
                    guard let raw = note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
                          AVAudioSession.InterruptionType(rawValue: raw) == .began else { return }
                    self?.queue.async { self?.fail(token: token, issue: .interrupted) }
                },
                center.addObserver(forName: AVAudioSession.routeChangeNotification, object: session, queue: nil) { [weak self] _ in
                    self?.queue.async { self?.fail(token: token, issue: .routechanged) }
                },
                center.addObserver(forName: UIApplication.didEnterBackgroundNotification, object: nil, queue: nil) { [weak self] _ in
                    self?.queue.async { self?.fail(token: token, issue: .interrupted) }
                }
            ]
            capture.prepare()
            try capture.start()
        } catch { fail(token: token, issue: .unavailable) }
    }

    private func fail(token: UInt64, issue: CaptureIssue) {
        // Called on the capture control queue, never while an engine tap is blocked.
        lock.lock()
        guard generation == token else { lock.unlock(); return }
        generation &+= 1
        let invalidated = generation
        let recipient = sink
        sink = nil
        lock.unlock()
        closeEngine()
        lock.lock()
        if generation == invalidated { recipient?.onIssue(issue: issue) }
        lock.unlock()
    }

    func stop() {
        lock.lock()
        generation &+= 1
        sink = nil
        lock.unlock()
        // Do not hold the callback lock while removeTap/stop wait for the engine tap.
        if DispatchQueue.getSpecific(key: queueKey) == true { closeEngine() }
        else { queue.sync { closeEngine() } }
    }

    private func closeEngine() {
        observers.forEach { NotificationCenter.default.removeObserver($0) }
        observers.removeAll()
        if let engine {
            engine.inputNode.removeTap(onBus: 0)
            engine.stop()
        }
        engine = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
    deinit { stop() }
}
