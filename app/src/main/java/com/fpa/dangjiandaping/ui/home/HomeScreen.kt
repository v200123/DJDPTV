package com.fpa.dangjiandaping.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
internal fun HomeScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF8E0000)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "原生首页内容区域",
            color = Color(0xFFFFD36A),
            fontSize = 28.sp
        )
    }
}

@Preview(
    name = "原生首页",
    widthDp = 1280,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(Modifier.fillMaxSize())
    }
}
