package com.storyteller_f.bao_library

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File

fun Context.defaultBaoHandler(it: Throwable?): Boolean {
    Thread {
        Thread.sleep(500)
        startActivity(Intent(this, ExceptionActivity::class.java).apply {
            putExtra(Bao.exceptionKey, it)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }.start()
    return true
}

fun Context.stringBaoHandler(it: Throwable?): Boolean {
    return if (it != null) {
        Bao.nativeExceptionFile(this).writeText(it.stackTraceToString())
        true
    } else {
        defaultBaoHandler(null)
    }
}

/**
 * Throwable 如果为null，说明异常信息存储在cache.txt 中。使用`Bao.readException(context)` 读取。
 */
class Bao(val app: Context, val block: Context.(Throwable?) -> Boolean = Context::stringBaoHandler) {
    private val file by lazy {
        nativeExceptionFile(app)
    }
    private val handler = Handler(Looper.getMainLooper())
    private val observer by lazy {
        if (!file.exists()) file.createNewFile()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(file), IFileWatcher {
                override fun onEvent(event: Int, path: String?) {
                    Log.d(TAG, "onEvent() called with: event = $event, path = $path")
                    if (event == CLOSE_WRITE) fileWriteDone()
                }

                override fun start() = startWatching()
                override fun stop() = stopWatching()
            }
        } else {
            object : FileWatcher(file.absolutePath) {
                override fun onEvent(event: Long) {
                    Log.d(TAG, "onEvent() called with: event = $event")
                    if (event == 2L) fileWriteDone()
                }

            }
        }
    }

    private fun fileWriteDone() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            app.block(null)
        }, 200)
    }

    fun bao() {
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (e: Exception) {
                    if (!app.block(e)) {
                        throw e
                    }
                }
            }
        }
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Log.i(TAG, "onCreate: ${Thread.currentThread()}")
            Log.i(TAG, "onCreate: $t")
            if (!app.block(e)) {
                old?.uncaughtException(t, e)
            }
        }
        observer.start()
        transferNativeExceptionFilePath(file.absolutePath)
        repeat(32) {
            registerActionHandler(it)
        }
    }

    companion object {
        private const val TAG = "Bao"
        const val exceptionKey = "exception"
        private const val nativeExceptionFileName = "catch.txt"

        external fun registerActionHandler(signal: Int)
        external fun transferNativeExceptionFilePath(path: String)

        fun readException(context: Context): String {
            return nativeExceptionFile(context).readText()
        }

        internal fun nativeExceptionFile(context: Context) =
            File(context.cacheDir, nativeExceptionFileName)

        init {
            System.loadLibrary("bao")
        }
    }
}
