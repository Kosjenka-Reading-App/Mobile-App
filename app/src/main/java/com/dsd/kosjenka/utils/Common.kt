package com.dsd.kosjenka.utils

import android.app.Activity
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity

class Common {

    companion object {

        fun hideSoftKeyboard(activity: Activity) {
            val inputMethodManager: InputMethodManager = activity.getSystemService(
                AppCompatActivity.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            if (inputMethodManager.isAcceptingText) {
                val currentFocus = activity.currentFocus
                if (currentFocus != null)
                    inputMethodManager.hideSoftInputFromWindow(
                        currentFocus.windowToken,
                        0
                    )
            }
        }

    }

}