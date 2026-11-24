package com.tomtune.audio

/** Short recursive callback gate; never hold during analysis or device I/O. */
internal expect class CaptureLock() {
    fun <T> withLock(action: () -> T): T
}
