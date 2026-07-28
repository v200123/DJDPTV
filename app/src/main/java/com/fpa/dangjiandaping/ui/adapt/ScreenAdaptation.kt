package com.fpa.dangjiandaping.ui.adapt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.min

private const val DESIGN_WIDTH_DP = 960f
private const val DESIGN_HEIGHT_DP = 540f

/**
 * 返回按设计稿比例缩放后的 Density。
 *
 * 使用较短边的缩放比例，避免非 16:9 屏幕把文字、图标或圆角拉伸变形。
 */
fun Density.adaptToDesign(scale: Float): Density = Density(
    density = density * scale,
    fontScale = fontScale,
)

/**
 * 为子树应用 960 × 540 设计稿的等比屏幕适配。
 *
 * 放在应用根部后，子树内既有的 dp、sp 会自动使用缩放后的 Density，
 * 无需在每一个组件中重复计算适配比例。
 */
@Composable
fun ProvideScreenAdaptation(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scale = remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        min(
            configuration.screenWidthDp / DESIGN_WIDTH_DP,
            configuration.screenHeightDp / DESIGN_HEIGHT_DP,
        )
    }
    val adaptedDensity = remember(density, scale) { density.adaptToDesign(scale) }

    CompositionLocalProvider(LocalDensity provides adaptedDensity, content = content)
}
