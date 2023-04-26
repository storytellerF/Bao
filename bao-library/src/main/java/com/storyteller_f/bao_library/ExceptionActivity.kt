package com.storyteller_f.bao_library

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.text.StaticLayout
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.DisplayCompat
import androidx.core.view.isVisible

class ExceptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exception)


        val exceptionText = findViewById<TextView>(R.id.text)
        val wrappedExceptionText = findViewById<TextView>(R.id.wrapped_text)
        val exception = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(Bao.exceptionKey, Throwable::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getSerializableExtra(Bao.exceptionKey) as? Throwable
        }

        val exceptionContent: CharSequence = exception?.stackTraceToString() ?: Bao.readException(this)
        exceptionText.text = exceptionContent
        wrappedExceptionText.text = exceptionContent

        val systemService = ContextCompat.getSystemService(this, ClipboardManager::class.java)
        findViewById<ImageButton>(R.id.copy).setOnClickListener {
            if (systemService != null) {
                systemService.setPrimaryClip(
                    ClipData.newPlainText(
                        "exception content",
                        exceptionContent
                    )
                )
            } else Toast.makeText(this, "no clipboard", Toast.LENGTH_SHORT).show()
        }
        findViewById<ImageButton>(R.id.wrap_text).setOnClickListener {
            wrappedExceptionText.isVisible = !wrappedExceptionText.isVisible
            exceptionText.isVisible = !exceptionText.isVisible
        }
    }
}