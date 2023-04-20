package com.storyteller_f.bao_library

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ExceptionActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exception)

        val exceptionText = findViewById<TextView>(R.id.text)
        val exception = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(Bao.exceptionKey, Throwable::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getSerializableExtra(Bao.exceptionKey) as? Throwable
        }

        if (exception != null) {
            exceptionText.text = exception.stackTraceToString()
        } else {
            exceptionText.text = Bao.readException(this)
        }
    }
}