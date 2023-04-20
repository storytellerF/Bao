package com.storyteller_f.bao_library

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File


class Bao(val app: Application, val block: (Throwable?) -> Boolean) {
    private val file by lazy {
        val file = File(app.cacheDir, nativeExceptionFileName)
        file
    }
    private val handler = Handler(Looper.getMainLooper())
    private val observer by lazy {
        if (!file.exists()) file.createNewFile()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(file) {
                override fun onEvent(event: Int, path: String?) {
                    Log.d(TAG, "onEvent() called with: event = $event, path = $path")
                    if (event == CLOSE_WRITE) {
                        handler.removeCallbacksAndMessages(null)
                        handler.postDelayed({
                            block(null)
                        }, 200)
                    }
                }

            }
        } else {
            TODO("VERSION.SDK_INT < Q")
        }
    }

    fun bao() {
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (e: Exception) {
                    if (!block(e)) {
                        throw e
                    }
                }
            }
        }
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Log.i(TAG, "onCreate: ${Thread.currentThread()}")
            Log.i(TAG, "onCreate: $t")
            if (!block(e)) {
                old?.uncaughtException(t, e)
            }
        }
        observer.startWatching()
        registerActionHandler(file.absolutePath)
    }

    companion object {
        private const val TAG = "Bao"
        const val exceptionKey = "exception"
        private const val nativeExceptionFileName = "catch.txt"

        external fun registerActionHandler(exceptionFilePath: String)

        fun readException(context: Context): String {
            return File(context.cacheDir, nativeExceptionFileName).readText()
        }

        init {
            System.loadLibrary("bao")
        }
    }
}