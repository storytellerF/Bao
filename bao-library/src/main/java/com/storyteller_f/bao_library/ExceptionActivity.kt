package com.storyteller_f.bao_library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import java.io.Serializable

class ExceptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val typeValue = TypedValue()
        val getThemeResult = theme.resolveAttribute(R.attr.exceptionPageTheme, typeValue, false)
        if (getThemeResult) {
            setTheme(typeValue.data)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exception)

        val exceptionText = findViewById<TextView>(R.id.text)
        val wrappedExceptionText = findViewById<TextView>(R.id.wrapped_text)
        val exception = getThrowable()

        val exceptionContent: CharSequence =
            exception?.stackTraceToString() ?: Bao.readException(this)
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

    private fun getThrowable() = intent.getSerializableExtraCompat(
        Bao.exceptionKey,
        Throwable::class.java
    )

    @Suppress("SameParameterValue")
    private fun <T : Serializable> Intent?.getSerializableExtraCompat(
        exceptionKey: String,
        clazz: Class<T>
    ): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this?.getSerializableExtra(exceptionKey, clazz)
        } else {
            @Suppress("DEPRECATION")
            clazz.cast(this?.getSerializableExtra(exceptionKey))
        }
    }
}