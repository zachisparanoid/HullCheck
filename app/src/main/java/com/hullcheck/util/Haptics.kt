package com.hullcheck.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** A subtle confirmation tick used when a checklist item status changes. */
fun View.performStatusTick() {
    val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        @Suppress("DEPRECATION")
        HapticFeedbackConstants.KEYBOARD_TAP
    }
    performHapticFeedback(constant)
}
