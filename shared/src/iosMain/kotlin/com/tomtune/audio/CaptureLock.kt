package com.tomtune.audio
import platform.Foundation.NSRecursiveLock
internal actual class CaptureLock actual constructor() {
    private val lock = NSRecursiveLock()
    actual fun <T> withLock(action: () -> T): T {
        lock.lock()
        try { return action() } finally { lock.unlock() }
    }
}
