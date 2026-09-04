package com.iumrah.beta.core.design

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object IumrahHaptics {
    fun soft(view: View) {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.GESTURE_START else HapticFeedbackConstants.KEYBOARD_TAP
        )
    }

    fun selection(view: View) {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 26) HapticFeedbackConstants.CLOCK_TICK else HapticFeedbackConstants.KEYBOARD_TAP
        )
    }

    fun success(view: View) {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        )
    }

    fun error(view: View) {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        )
    }
}
