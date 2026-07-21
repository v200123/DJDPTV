package com.fpa.dangjiandaping.ui.header

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
internal fun NativeHeader(
    selectedTab: Int,
    focusedTab: Int,
    tabFocusRequesters: List<FocusRequester>,
    onTabFocused: (Int) -> Unit,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(
            Brush.horizontalGradient(
                listOf(
                    Color(0xFFB00000),
                    Color(0xFFDF130B),
                    Color(0xFFA60000)
                )
            )
        )
    ) {
        HeaderBrandRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.54f)
                .padding(horizontal = 28.dp)
        )
        HeaderDivider()
        HeaderTabRow(
            selectedTab = selectedTab,
            focusedTab = focusedTab,
            tabFocusRequesters = tabFocusRequesters,
            onTabFocused = onTabFocused,
            onTabSelected = onTabSelected,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.46f)
                .padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun HeaderBrandRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "☭",
            color = Color(0xFFFFD36A),
            fontSize = 34.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "康巴党旗红数字党建平台",
            color = Color(0xFFFFD36A),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text("中共甘孜州委组织部", color = Color(0xFFFFD186), fontSize = 22.sp)
            Text(currentDateText(), color = Color(0xFFFFD186), fontSize = 10.sp)
        }
    }
}

@Composable
private fun HeaderDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0xFFFFD36A),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun HeaderTabRow(
    selectedTab: Int,
    focusedTab: Int,
    tabFocusRequesters: List<FocusRequester>,
    onTabFocused: (Int) -> Unit,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var tabRowHasFocus by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .focusRestorer()
            .onFocusChanged { tabRowHasFocus = it.hasFocus },
        verticalAlignment = Alignment.CenterVertically
    ) {
        TV_TABS.forEachIndexed { index, tab ->
            val focused = tabRowHasFocus && focusedTab == index
            val retainedFocus = !tabRowHasFocus && focusedTab == index

            Box(
                modifier = Modifier
                    .weight(tab.widthWeight)
                    .fillMaxHeight()
                    .focusRequester(tabFocusRequesters[index])
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onTabFocused(index)
                            Log.d(FOCUS_LOG_TAG, "NativeTab[$index][${tab.title}] FOCUSED")
                        }
                    }
                    .selectable(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        role = Role.Tab
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
        modifier = modifier.size(width = 56.dp, height = 9.dp)
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
    val tabScale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        label = "tabScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(
                    start = 5.dp,
                    top = if (recommended) 7.dp else 5.dp,
                    end = if (recommended) 12.dp else 5.dp,
                    bottom = 5.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (emphasized) Color.White else Color(0xFFEBCACA),
                fontSize = if (emphasized) 22.sp else 16.sp,
                fontStyle = if (focused) FontStyle.Italic else FontStyle.Normal,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.scale(tabScale)
            )
        }

        if (emphasized) {
            TabBrushIndicator(
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (recommended) {
            Text(
                text = "推荐",
                color = Color(0xFFFFD36A),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .background(Color(0xFFD91C12), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFFFD36A), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
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
            modifier = Modifier.fillMaxSize()
        )
    }
}
