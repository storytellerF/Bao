package com.storyteller_f.bao_library

interface IFileWatcher {
    fun start()
    fun stop()
}

abstract class FileWatcher(private val path: String) : IFileWatcher {
    private var nativePtr: Int = -1
    override fun start() {
        nativePtr = startNative(path)
    }

    override fun stop() {
        if (nativePtr != -1)
            stopNative(nativePtr);
    }

    abstract fun onEvent(event: Long)
    protected fun finalize() {
        stop()
    }

    private external fun startNative(path: String): Int
    private external fun stopNative(ptr: Int)

    companion object {

        init {
            System.loadLibrary("file-watcher")
        }
    }
}