package com.dsd.kosjenka.utils

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dsd.kosjenka.R
import com.google.android.material.snackbar.Snackbar

class Common {

    companion object {

        fun hideSoftKeyboard(activity: Activity) {
            val inputMethodManager: InputMethodManager = activity.getSystemService(
                AppCompatActivity.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            if (inputMethodManager.isAcceptingText) {
                val currentFocus = activity.currentFocus
                if (currentFocus != null) inputMethodManager.hideSoftInputFromWindow(
                    currentFocus.windowToken, 0
                )
            }
        }

        fun showSnackBar(message: String, view: View) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).setBackgroundTint(
                ContextCompat.getColor(
                    view.context, R.color.secondary_background
                )
            ).setTextColor(ContextCompat.getColor(view.context, R.color.colorPrimary))
                .setTextMaxLines(5).show()
        }

        fun showToast(context: Context, message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }


        fun String.isValidEmail(): Boolean {
            return !TextUtils.isEmpty(this) && Patterns.EMAIL_ADDRESS.matcher(this).matches()
        }
    }
}