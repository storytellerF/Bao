package com.storyteller_f.bao

import android.app.Application
import android.content.Intent
import com.storyteller_f.bao_library.Bao
import com.storyteller_f.bao_library.ExceptionActivity

class App: Application() {
    val bao = Bao(this)
    override fun onCreate() {
        super.onCreate()
        bao.bao()
    }
}