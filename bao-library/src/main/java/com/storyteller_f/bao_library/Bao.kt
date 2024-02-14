package com.storyteller_f.bao_library

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import dalvik.annotation.optimization.CriticalNative
import dalvik.annotation.optimization.FastNative
import java.io.File
import java.lang.reflect.InvocationTargetException
import kotlin.concurrent.thread

/**
 * 默认返回true
 */
fun Context.defaultBaoHandler(it: Throwable?): Boolean {
    Log.d("Bao", "defaultBaoHandler() called with: it = $it")
    thread {
        startActivity(Intent(this, ExceptionActivity::class.java).apply {
            putExtra(Bao.EXCEPTION_KEY, it?.stackTraceToString() ?: Bao.readException(this@defaultBaoHandler))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
    return true
}

/**
 * Throwable 如果为null，说明异常信息存储在cache.txt 中。使用`Bao.readException(context)` 读取。
 * @param block 返回值代表异常是否被处理了
 */
class Bao(
    private val app: Context,
    val block: Context.(Throwable?) -> Boolean = Context::defaultBaoHandler
) {
    private val file by lazy {
        nativeExceptionFile(app)
    }
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 监听native 是否将异常信息写入到文件中
     */
    private val observer by lazy {
        if (!file.exists()) file.createNewFile()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(file), IFileWatcher {
                override fun onEvent(event: Int, path: String?) {
                    Log.d(TAG, "onEvent() called with: event = $event, path = $path")
                    if (event == CLOSE_WRITE) nativeFileWriteDone()
                }

                override fun start() = startWatching()
                override fun stop() = stopWatching()
            }
        } else {
            object : FileWatcher(file.absolutePath) {
                override fun onEvent(event: Int) {
                    Log.d(TAG, "onEvent() called with: event = $event")
                    if (event == 2) nativeFileWriteDone()
                }

            }
        }
    }

    private fun nativeFileWriteDone() {
        val caught = isCaught()
        Log.d(TAG, "nativeFileWriteDone() called $caught")
        if (caught) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                app.block(null)
            }, 200)
        }
    }

    fun bao() {
        Handler(Looper.getMainLooper()).postAtFrontOfQueue {
            while (true) {
                Log.v(TAG, "bao: loop start")
                try {
                    Looper.loop()
                } catch (e: Throwable) {
                    Log.i(TAG, "bao: loop", e)
                    handleOrRethrow(e)?.let {
                        throw it
                    }
                }
            }
        }
        //fixme 无法开启loop
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Log.i(TAG, "bao: defaultHandler $t $e")
            //如果是在loop 中抛出，会附带zygote 的崩溃调用栈，需要获取真正崩溃的原因
            val exception =
                ((e as? RuntimeException)?.cause as? InvocationTargetException)?.targetException
                    ?: e
            handleOrRethrow(exception)?.let {
                old?.uncaughtException(t, it)
            }
        }
        observer.start()
        transferNativeExceptionFilePath(file.absolutePath)
        LinuxSig.appFatalSig.forEach {
            registerActionHandler(it)
        }
    }

    private fun handleOrRethrow(e: Throwable): Throwable? {
        Log.d(TAG, "handleOrRethrow() called with: e = $e")

        return if (e is TwoException) {
            e
        } else if (notifyExceptionCaught()) {
            if (app.block(e)) {
                Log.e(TAG, "handleOrRethrow: 此异常被Bao 处理", e)
                null
            } else {
                Exception("app.block(e) 未正常处理", e)
            }
        } else {
            TwoException(e, "二次崩溃，第一次崩溃请向前翻阅")
        }
    }

    companion object {
        private const val TAG = "Bao"
        const val EXCEPTION_KEY = "exception"
        private const val NATIVE_EXCEPTION_FILENAME = "catch.txt"


        fun readException(context: Context) = nativeExceptionFile(context).readText()

        internal fun nativeExceptionFile(context: Context) =
            File(context.externalCacheDir, NATIVE_EXCEPTION_FILENAME)

        init {
            System.loadLibrary("bao")
        }
    }
}

class TwoException(override val cause: Throwable?, override val message: String?) : Exception(cause)

external fun registerActionHandler(signal: Int)

@FastNative
external fun transferNativeExceptionFilePath(path: String)

/**
 * 正在处理的异常只有一个，标志存储在native 环境中。
 * @return 返回是否需要处理当前异常。比如当前已经存在一个异常了，后续的会被忽略掉
 */
@CriticalNative
external fun notifyExceptionCaught(): Boolean

@CriticalNative
external fun notifyExceptionHandled()

/**
 * @return 返回当前是不是正在处理异常中
 */
@CriticalNative
external fun isCaught(): Boolean