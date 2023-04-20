package com.storyteller_f.bao

import android.app.Application
import android.content.Intent
import com.storyteller_f.bao_library.Bao
import com.storyteller_f.bao_library.ExceptionActivity

class App: Application() {
    val bao = Bao(this) {
        Thread {
            Thread.sleep(500)
            startActivity(Intent(this@App, ExceptionActivity::class.java).apply {
                putExtra(Bao.exceptionKey, it)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }.start()
        true
    }
    override fun onCreate() {
        super.onCreate()
        bao.bao()
    }

    companion object
}