package com.fpa.dangjiandaping.ui.focus

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager

/**
 * Keeps pointer interaction and TV remote navigation in the same focus system.
 *
 * Compose enters touch mode for pointer events. TV controls may reject or stop drawing focus in
 * that mode, so switch back to keyboard/remote mode before requesting focus.
 */
@Composable
fun Modifier.focusOnClick(focusRequester: FocusRequester): Modifier {
    val inputModeManager = LocalInputModeManager.current
    return pointerInput(focusRequester, inputModeManager) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            inputModeManager.requestInputMode(InputMode.Keyboard)
            focusRequester.requestFocus()
        }
    }
}
