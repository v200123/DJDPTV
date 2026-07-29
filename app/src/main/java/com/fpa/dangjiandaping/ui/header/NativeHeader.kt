package com.fpa.dangjiandaping.ui.header

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fpa.dangjiandaping.R
import com.fpa.dangjiandaping.ui.navigation.TV_TABS
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FOCUS_LOG_TAG = "FocusTrace"

@Composable
internal fun rememberTvTabFocusRequesters(): List<FocusRequester> =
    remember { List(TV_TABS.size) { FocusRequester() } }

@Composable
fun NativeHeader(
    selectedTab: Int,
    focusedTab: Int,
    tabFocusRequesters: List<FocusRequester>,
    onTabFocused: (Int) -> Unit,
    onTabSelected: (Int) -> Unit,
    onTabDown: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        HeaderBrandRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )
        HeaderDivider()
        HeaderTabRow(
            selectedTab = selectedTab,
            focusedTab = focusedTab,
            tabFocusRequesters = tabFocusRequesters,
            onTabFocused = onTabFocused,
            onTabSelected = onTabSelected,
            onTabDown = onTabDown,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Composable
private fun HeaderBrandRow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopEnd
    ) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.offset(0.dp,10.dp)) {
            Text("中共甘孜州委组织部", color = Color(0xFFFFD186), fontSize = 12.sp)
            Text(currentDateText(), color = Color(0xFFFFD186), fontSize = 10.sp)
        }
    }
}

@Composable
private fun HeaderDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
    )
}

@Composable
private fun HeaderTabRow(
    selectedTab: Int,
    focusedTab: Int,
    tabFocusRequesters: List<FocusRequester>,
    onTabFocused: (Int) -> Unit,
    onTabSelected: (Int) -> Unit,
    onTabDown: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var tabRowHasFocus by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .padding(51.dp,0.dp)
            .focusRestorer()
            .onFocusChanged { tabRowHasFocus = it.hasFocus },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TV_TABS.forEachIndexed { index, tab ->
            val focused = tabRowHasFocus && focusedTab == index
            val retainedFocus = !tabRowHasFocus && focusedTab == index
            Box(
                modifier = Modifier
                    .focusRequester(tabFocusRequesters[index])
                    .focusProperties {
                        up = FocusRequester.Cancel
                        if (index == 0) {
                            left = FocusRequester.Cancel
                        }
                        if (index == TV_TABS.lastIndex) {
                            right = FocusRequester.Cancel
                        }
                    }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onTabFocused(index)
                            Log.d(FOCUS_LOG_TAG, "NativeTab[$index][${tab.title}] FOCUSED")
                        } }
                    .onPreviewKeyEvent { event ->
                        val moveToContent = event.type == KeyEventType.KeyDown &&
                            event.key in setOf(
                                Key.DirectionDown,
                                Key.Enter,
                                Key.NumPadEnter,
                                Key.DirectionCenter,
                            )
                        if (moveToContent) {
                            onTabDown(index)
                            true
                        } else {
                            false
                        }
                    }
                    .selectable(
                        selected = selectedTab == index,
                        interactionSource = null,
                        onClick = {
                            tabFocusRequesters[index].requestFocus()
                            onTabSelected(index)
                        },
                        role = Role.Tab,
                        indication = null,
                    ),
                contentAlignment = Alignment.Center
            ) {
                TvTabContent(
                    text = tab.title,
                    recommended = tab.recommended,
                    focused = focused,
                    retainedFocus = retainedFocus
                )
            }
        }
    }
}

@Composable
private fun TabBrushIndicator(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_top_tab_ind),
        contentDescription = null,
        contentScale = ContentScale.None,
        modifier = modifier
            .size(width = 53.dp, height = 9.dp)
            .offset(y = 8.dp)
    )
}

@Composable
private fun TvTabContent(
    text: String,
    recommended: Boolean,
    focused: Boolean,
    retainedFocus: Boolean
) {
    val emphasized = focused || retainedFocus
//    val tabScale by animateFloatAsState(
//        targetValue = if (focused) 1.06f else 1f,
//        label = "tabScale"
//    )
    Box(
        modifier = Modifier
    ) {
        StableTabLabel(
            text = text,
            emphasized = emphasized,
            focused = focused,
        )

        if (emphasized) {
            TabBrushIndicator(
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

//        if (recommended) {
//            val badgeOffsetX = if (emphasized) 12.dp else 3.dp
//            val badgeOffsetY = if (emphasized) (-7).dp else (-3).dp
//            Text(
//                text = "推荐",
//                color = Color(0xFFFFD36A),
//                fontSize = 9.sp,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier
//                    .align(Alignment.TopEnd)
//                    .offset(x = badgeOffsetX, y = badgeOffsetY)
//                    .background(Color(0xFFD91C12), RoundedCornerShape(4.dp))
//                    .border(1.dp, Color(0xFFFFD36A), RoundedCornerShape(4.dp))
//                    .padding(horizontal = 4.dp, vertical = 1.dp)
//            )
//        }
    }
}

@Composable
private fun StableTabLabel(text: String, emphasized: Boolean, focused: Boolean) {
    Layout(
        content = {
            // The normal label determines horizontal placement for every tab.
            Text(
                text = text,
                color = Color.Transparent,
                fontSize = 16.sp,
                fontStyle = FontStyle.Normal,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            // Retain the maximum height for the visual-scale overflow and indicator.
            Text(
                text = text,
                color = Color.Transparent,
                fontSize = 22.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = text,
                color = if (emphasized) Color.White else Color(0xFFEBCACA),
                fontSize = 16.sp,
                fontStyle = if (focused) FontStyle.Italic else FontStyle.Normal,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer {
                    val scale = if (emphasized) 22f / 16f else 1f
                    scaleX = scale
                    scaleY = scale
                }
                    .then(
                        if (focused) {
                            Modifier.border(
                                width = 2.dp,
                                color = Color(0xFFFFD186),
                                shape = RoundedCornerShape(5.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        },
    ) { measurables, constraints ->
        val normal = measurables[0].measure(constraints)
        val maximum = measurables[1].measure(constraints)
        val current = measurables[2].measure(constraints)
        val height = maximum.height

        layout(normal.width, height) {
            normal.placeRelative(0, (height - normal.height) / 2)
            maximum.placeRelative(0, 0)
            current.placeRelative(0, (height - current.height) / 2)
        }
    }
}

private fun currentDateText(): String =
    SimpleDateFormat("yyyy年M月d日  EEEE", Locale.CHINA).format(Date())

@Preview(
    name = "顶部导航栏",
    widthDp = 1280,
    heightDp = 120,
    showBackground = true
)
@Composable
private fun NativeHeaderPreview() {
    var selectedTab by remember { mutableStateOf(0) }
    var focusedTab by remember { mutableStateOf(0) }
    val focusRequesters = rememberTvTabFocusRequesters()

    MaterialTheme {
        NativeHeader(
            selectedTab = selectedTab,
            focusedTab = focusedTab,
            tabFocusRequesters = focusRequesters,
            onTabFocused = { focusedTab = it },
            onTabSelected = {
                selectedTab = it
                focusedTab = it
            },
            onTabDown = {
                selectedTab = it
                focusedTab = it
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
