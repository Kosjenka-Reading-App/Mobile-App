package com.dsd.kosjenka.presentation.home.exercise

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.NestedScrollView

class HighlightTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    private var currentIndex = -1
    private var resetCallback = false
    private var reachedEnd = false
    private var highlightCallback: HighlightCallback? = null
    private val highlightSpan = ForegroundColorSpan(Color.YELLOW)
    private lateinit var wordList: List<String>

    fun highlightNextWord() {
        if(reachedEnd)
            return

        if (!this::wordList.isInitialized) wordList = text.split(" ")

        if (!resetCallback) {
            // Increment index and check if it's valid
            if (++currentIndex < wordList.size) {
                // Clear previous and highlight current word
                clearPreviousHighlight()
                highlightCurrentWord()

                scrollToWord(currentIndex)
            } else {
                // Reached the end, reset index
                currentIndex = -1
                reachedEnd = true
                highlightCallback?.onHighlightEnd()
            }
        } else {
            resetCallback = false
        }
    }

    fun setHighlightCallback(callback: HighlightCallback?) {
        highlightCallback = callback
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

    private fun scrollToWord(index: Int) {
        val layout = layout ?: return
        val startIndex = wordList.take(index).sumOf { it.length + 1 } // +1 for the space
        val endIndex = startIndex + wordList[index].length
        val startY = layout.getLineTop(layout.getLineForOffset(startIndex.toInt()))
        val endY = layout.getLineBottom(layout.getLineForOffset(endIndex.toInt()))
        val viewHeight = height

        // Calculate the scroll position to center the text vertically
        val scrollY = (startY + endY - viewHeight) / 2

        // Get the parent NestedScrollView (adjust the type if needed)
        val parentScrollView = parent as? NestedScrollView

        // Scroll smoothly to the calculated position
        parentScrollView?.smoothScrollTo(0, scrollY)
    }



}
