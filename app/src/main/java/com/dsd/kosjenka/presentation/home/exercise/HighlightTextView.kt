package com.dsd.kosjenka.presentation.home.exercise

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class HighlightTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {
    private var currentIndex = -1
    private val highlightSpan = ForegroundColorSpan(Color.YELLOW)
    private lateinit var wordList: List<String>

    fun highlightNextWord() {
        currentIndex++

        // Remove the previous highlight
        clearPreviousHighlight()

        if (!this::wordList.isInitialized) wordList = text.split(" ")

        // Check if we reached the end of the text
        if (currentIndex < wordList.size) {
            // Highlight the current word
            highlightCurrentWord()
        }
    }

    private fun clearPreviousHighlight() {
        if (currentIndex >= 0) {
            val spannableText = SpannableStringBuilder(text)
            val spans =
                spannableText.getSpans(0, spannableText.length, BackgroundColorSpan::class.java)

            for (span in spans) {
                spannableText.removeSpan(span)
            }

            text = spannableText
        }
    }

    private fun highlightCurrentWord() {
        if (currentIndex >= 0 && currentIndex < wordList.size) {
            val startIndex = wordList.take(currentIndex).sumOf { it.length + 1 } // +1 for the space
            val endIndex = startIndex + wordList[currentIndex].length

            val spannableText = SpannableStringBuilder(text)
            spannableText.setSpan(
                highlightSpan, startIndex, endIndex, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            text = spannableText
        }
    }
}
