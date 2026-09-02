package com.fpa.dangjiandaping.ui.focus

import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

private const val FOCUS_LOG_TAG = "ComposeFocus"

/** Emits every focus-state transition for a named Compose focus target. */
fun Modifier.logFocusTarget(name: String): Modifier = onFocusChanged { state ->
    Log.d(
        FOCUS_LOG_TAG,
        "$name focused=${state.isFocused}, hasFocus=${state.hasFocus}, captured=${state.isCaptured}",
    )
}
