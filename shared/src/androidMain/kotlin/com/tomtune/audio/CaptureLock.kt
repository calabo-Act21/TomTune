package com.tomtune.audio
internal actual class CaptureLock actual constructor() {
    private val monitor = Any()
    actual fun <T> withLock(action: () -> T): T = synchronized(monitor, action)
}
