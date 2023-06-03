package com.storyteller_f.bao_library

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File

@Suppress("unused")
object LinuxSig {
    const val hup = 1
    const val int = 2
    const val quit = 3
    const val ill = 4
    const val trap = 5
    const val abrt = 6
    const val bus = 7
    const val fpe = 8
    const val kill = 9
    const val usr1 = 10
    const val segv = 11
    const val usr2 = 12
    const val pipe = 13
    const val alrm = 14
    const val term = 15
    const val stkflt = 16
    const val chld = 17
    const val cont = 18
    const val stop = 19
    const val tstp = 20
    const val ttin = 21
    const val ttou = 22
    const val urg = 23
    const val xcpu = 24
    const val xfsz = 25
    const val vtalrm = 26
    const val prof = 27
    const val winch = 28
    const val io = 29
    const val pwr = 30
    const val sys = 31
    val appFatalSig = listOf(abrt, bus, fpe, ill, int, pipe, segv, term)
    val ignoreSig = listOf(chld, urg, winch)
    val terminateSig = listOf(hup, int, kill, usr1, usr1, usr2, pipe, alrm, term, stkflt, vtalrm, prof, io, pwr)
    val coreSig = listOf(quit, ill, trap, abrt, bus, fpe, segv, xcpu, xfsz, sys)
    val stopSig = listOf(stop, tstp, ttin, ttou)
    val continueSig = listOf(cont)
}

fun Context.defaultBaoHandler(it: Throwable?): Boolean {
    Log.d("Bao", "defaultBaoHandler() called with: it = $it")
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
    Log.d("Bao", "stringBaoHandler() called with: it = $it")
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
class Bao(
    private val app: Context,
    val block: Context.(Throwable?) -> Boolean = Context::stringBaoHandler
) {
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
        Handler(Looper.getMainLooper()).postAtFrontOfQueue {
            while (true) {
                Log.v(TAG, "bao: loop start")
                try {
                    Looper.loop()
                } catch (e: Throwable) {
                    Log.i(TAG, "bao: loop $e")
                    if (!app.block(e)) {
                        throw e
                    }
                }
            }
        }
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Log.i(TAG, "bao: current ${Thread.currentThread()}")
            Log.i(TAG, "bao: input $t")
            if (!app.block(e)) {
                old?.uncaughtException(t, e)
            }
        }
        observer.start()
        transferNativeExceptionFilePath(file.absolutePath)
        LinuxSig.appFatalSig.forEach {
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
