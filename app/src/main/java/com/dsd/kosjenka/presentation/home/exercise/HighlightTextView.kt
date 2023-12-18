package com.dsd.kosjenka.presentation.home.exercise

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class HighlightTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    private var currentIndex = -1
    private var resetCallback = false
    private val highlightSpan = ForegroundColorSpan(Color.YELLOW)
    private lateinit var wordList: List<String>

    fun highlightNextWord() {
        if (!this::wordList.isInitialized) wordList = text.split(" ")

        if (!resetCallback) {
            // Increment index and check if it's valid
            if (++currentIndex < wordList.size) {
                // Clear previous and highlight current word
                clearPreviousHighlight()
                highlightCurrentWord()
            } else
            // Reached the end, reset index
                currentIndex = -1
        } else resetCallback = false
    }

    private fun clearPreviousHighlight() {
        if (currentIndex > 0) {
            val spannableText = SpannableStringBuilder(text)
            spannableText.removeSpan(highlightSpan)
            text = spannableText
        }
    }

    private fun highlightCurrentWord() {
        val startIndex = wordList.take(currentIndex).sumOf { it.length + 1 } // +1 for the space
        val endIndex = startIndex + wordList[currentIndex].length

        val spannableText = SpannableStringBuilder(text)
        spannableText.setSpan(
            highlightSpan, startIndex, endIndex, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        text = spannableText
    }

    fun resetCallback() {
        resetCallback = true
    }
}
