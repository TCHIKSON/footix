package com.footix.tv.core

import android.util.Log

/** Journal unique de l'application. A lire avec: adb logcat -s Footix */
object Logger {

    private const val TAG = "Footix"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun w(message: String, error: Throwable? = null) {
        if (error == null) Log.w(TAG, message) else Log.w(TAG, message, error)
    }
}
