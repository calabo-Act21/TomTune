import AppKit

let directory = URL(fileURLWithPath: FileManager.default.currentDirectoryPath).appendingPathComponent("docs/lot2-3/preuves/essai-floor14-2026-11-26")
let serial = try String(contentsOfFile: "/private/tmp/tomtune-tools/physical-device-serial", encoding: .utf8).trimmingCharacters(in: .whitespacesAndNewlines)
let app = NSApplication.shared
app.setActivationPolicy(.regular)

class Cues: NSObject, NSWindowDelegate {
    var window: NSWindow!
    let count = NSTextField(labelWithString: "Prêt")
    let instruction = NSTextField(wrappingLabelWithString: "Même vis · même baguette · même montage\nUn coup un peu plus franc à chaque TOP\nNe pas appuyer sur Suivant dans TomTune")
    var start: NSButton!
    var timer: Timer?
    var ordinal = 0
    var running = false
    var ended = false
    func marker(_ command: String, _ ordinal: Int = 0) -> Bool {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/private/tmp/tomtune-tools/android-sdk/platform-tools/adb")
        process.arguments = ["-s", serial, "shell", "am", "broadcast", "-a", "com.tomtune.app.DEBUG_LUG", "-p", "com.tomtune.app", "--es", "command", command, "--es", "block", "firm", "--ei", "ordinal", String(ordinal)]
        process.standardInput = FileHandle.nullDevice
        process.standardOutput = FileHandle.nullDevice
        process.standardError = FileHandle.nullDevice
        let before = Date().timeIntervalSince1970
        do { try process.run(); process.waitUntilExit() } catch { return false }
        let after = Date().timeIntervalSince1970
        let record: [String: Any] = ["command":command,"block":"firm","ordinal":ordinal,"hostBeforeEpochSeconds":before,"hostAfterEpochSeconds":after,"broadcastSeconds":after-before,"renderImmediatelyAfterBroadcast":true]
        if let data = try? JSONSerialization.data(withJSONObject: record, options: [.sortedKeys]), let line = String(data:data,encoding:.utf8) {
            let url = directory.appendingPathComponent("host-cues.jsonl")
            if !FileManager.default.fileExists(atPath:url.path) { FileManager.default.createFile(atPath:url.path,contents:nil) }
            if let handle = try? FileHandle(forWritingTo:url) { handle.seekToEndOfFile(); handle.write(Data((line+"\n").utf8)); try? handle.close() }
        }
        print("CUE command=\(command) block=firm ordinal=\(ordinal) status=\(process.terminationStatus)")
        fflush(stdout)
        return process.terminationStatus == 0
    }
    func show() {
        window = NSWindow(contentRect:NSRect(x:0,y:0,width:760,height:440),styleMask:[.titled,.closable],backing:.buffered,defer:false)
        window.title = "TomTune — 10 coups un peu plus francs"
        window.delegate = self
        window.center()
        count.font = .boldSystemFont(ofSize:72)
        count.alignment = .center
        count.frame = NSRect(x:20,y:230,width:720,height:100)
        instruction.font = .systemFont(ofSize:21)
        instruction.alignment = .center
        instruction.frame = NSRect(x:30,y:95,width:700,height:110)
        start = NSButton(title:"Démarrer les 10 tops",target:self,action:#selector(begin))
        start.bezelStyle = .rounded
        start.font = .boldSystemFont(ofSize:20)
        start.frame = NSRect(x:220,y:35,width:320,height:45)
        window.contentView?.addSubview(count)
        window.contentView?.addSubview(instruction)
        window.contentView?.addSubview(start)
        window.makeKeyAndOrderFront(nil)
        app.activate(ignoringOtherApps:true)
    }
    @objc func begin() {
        guard !running else { return }
        running = true; start.isEnabled = false
        guard marker("reset") else { fail(); return }
        var seconds = 5
        count.stringValue = "Silence · 5"
        timer = Timer.scheduledTimer(withTimeInterval:1,repeats:true) { [self] t in
            seconds -= 1
            if seconds > 0 { count.stringValue = "Silence · \(seconds)" }
            else {
                t.invalidate()
                guard marker("begin") else { fail(); return }
                top()
                timer = Timer.scheduledTimer(withTimeInterval:3,repeats:true) { [self] _ in top() }
            }
        }
    }
    func top() {
        if ordinal == 10 {
            timer?.invalidate()
            count.stringValue = "Silence"
            DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [self] in
                _ = marker("end"); ended = true; running = false
                count.stringValue = "Terminé"
                instruction.stringValue = "Ne frappe plus. Reviens au chat pour confirmer les 10 coups.\nGarde le même montage et laisse Suivant intact."
            }
            return
        }
        ordinal += 1
        guard marker("cue",ordinal) else { fail(); return }
        count.stringValue = "TOP \(ordinal) / 10"
        window.backgroundColor = NSColor.systemGreen.withAlphaComponent(0.30)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { [self] in window.backgroundColor = .windowBackgroundColor }
    }
    func fail() { timer?.invalidate(); count.stringValue = "Arrêt"; instruction.stringValue = "Liaison Android indisponible. Ne frappe pas."; running = false }
    func windowWillClose(_ notification: Notification) { timer?.invalidate(); if running && !ended { _ = marker("end") }; app.terminate(nil) }
}
let cues = Cues()
cues.show()
app.run()
