package com.cleanspace.utils

import android.util.Log

object AppLog {
    private const val TAG = "CleanSpace"

    fun d(msg: String) {
        Log.d(TAG, msg)
    }

    fun e(msg: String, tr: Throwable? = null) {
        Log.e(TAG, msg, tr)
    }
}
