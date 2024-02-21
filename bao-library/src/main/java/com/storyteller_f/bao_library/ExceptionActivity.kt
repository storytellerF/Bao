package com.storyteller_f.bao_library

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

class ExceptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("ExceptionActivity", "onCreate() called with: savedInstanceState = $savedInstanceState")
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exception)

        val exceptionText = findViewById<TextView>(R.id.text)
        val wrappedExceptionText = findViewById<TextView>(R.id.wrapped_text)
        val exception = getThrowable()

        val exceptionContent: CharSequence = exception ?: "Exception is null."
        flashContent(exceptionContent, exceptionText, wrappedExceptionText)

        setupEvent(exceptionContent, wrappedExceptionText, exceptionText)
    }

    override fun onDestroy() {
        super.onDestroy()
        notifyExceptionHandled()
    }

    private fun flashContent(
        exceptionContent: CharSequence,
        exceptionText: TextView,
        wrappedExceptionText: TextView
    ) {
        val spannedExceptionContent = SpannableStringBuilder(exceptionContent).apply {
            search(packageName) { start, end ->
                val endIndex = indexOf("\n", end)
                val lineEnd = if (endIndex > 0) {
                    endIndex
                } else end
                setSpan(
                    ForegroundColorSpan(Color.BLUE),
                    start,
                    lineEnd,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
        }
        exceptionText.text = spannedExceptionContent
        wrappedExceptionText.text = spannedExceptionContent
    }

    private fun setupEvent(
        exceptionContent: CharSequence,
        wrappedExceptionText: TextView,
        exceptionText: TextView
    ) {
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

    private fun applyTheme() {
        val typeValue = TypedValue()
        val getThemeResult = theme.resolveAttribute(R.attr.exceptionPageTheme, typeValue, false)
        if (getThemeResult) {
            setTheme(typeValue.data)
        }
    }

    private fun SpannableStringBuilder.search(
        pattern: String,
        whenSearched: SpannableStringBuilder.(Int, Int) -> Unit
    ) {
        var start = 0
        while (true) {
            val indexOf = indexOf(pattern, start)
            if (indexOf < 0) {
                break
            }
            start = indexOf + 1
            whenSearched(indexOf, indexOf + pattern.length)
        }
    }

    private fun getThrowable() = intent.getStringExtra(Bao.EXCEPTION_KEY)

}