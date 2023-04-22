package com.storyteller_f.bao_library

import kotlin.concurrent.thread

interface IFileWatcher {
    fun start()
    fun stop()
}

abstract class FileWatcher(private val path: String) : IFileWatcher {
    override fun start() {
        thread {
            startNative(path)
        }
    }

    override fun stop() {
        stopNative()
    }

    abstract fun onEvent(event: Int)
    protected fun finalize() {
        stop()
    }

    private external fun startNative(path: String)
    private external fun stopNative()

    companion object {
        private const val TAG = "FileWatcher"

        init {
            System.loadLibrary("file-watcher")
        }
    }
}