package com.fpa.dangjiandaping.ui.web

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.ui.focus.focusOnClick
import com.fpa.dangjiandaping.ui.focus.logFocusTarget

internal val HelpDialogRed = Color(0xFF9F101B)
internal val HelpDialogDeepRed = Color(0xFF620810)
internal val HelpDialogCardRed = Color(0xB37A0D16)
internal val HelpDialogGold = Color(0xFFFFD27A)
internal val HelpDialogLightGold = Color(0xFFFFE7B2)
internal val HelpDialogWarmWhite = Color(0xFFFFF7ED)
internal val HelpDialogMutedText = Color(0xFFFFDAD2)
internal val HelpDialogGoldBorder = Color(0x99FFD27A)
internal val HelpDialogScrim = Color.Black.copy(alpha = 0.82f)

internal fun Modifier.tvDialogPanel(
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
): Modifier = this
    .clip(shape)
    .background(
        Brush.radialGradient(
            colors = listOf(HelpDialogRed, HelpDialogDeepRed),
            radius = 900f,
        ),
    )
    .border(1.dp, HelpDialogGoldBorder, shape)

@Composable
internal fun TvDialogCloseButton(
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.08f else 1f,
        label = "tvDialogCloseScale",
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { focused = it.isFocused }
            .clip(CircleShape)
            .background(if (focused) Color(0xFFD52B38) else Color(0xFFB62430))
            .border(if (focused) 3.dp else 2.dp, HelpDialogGold, CircleShape)
            .focusRequester(focusRequester)
            .logFocusTarget("Dialog.Close")
            .focusOnClick(focusRequester)
            .clickable(role = Role.Button) {
                focusRequester.requestFocus()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "×",
            color = Color.White,
            fontSize = 31.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
