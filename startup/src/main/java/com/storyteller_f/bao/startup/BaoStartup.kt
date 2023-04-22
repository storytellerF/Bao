package com.storyteller_f.bao.startup

import android.content.Context
import androidx.startup.Initializer
import com.storyteller_f.bao_library.Bao

class BaoStartup : Initializer<Bao> {
    override fun create(context: Context): Bao {
        return Bao(context).apply {
            bao()
        }
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}