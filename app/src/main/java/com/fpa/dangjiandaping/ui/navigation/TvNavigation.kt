package com.fpa.dangjiandaping.ui.navigation

private const val TEMPORARY_WEB_URL =
    "http://192.168.20.233:5173/xiaoyuTv/#/"

internal sealed interface TvTabDestination {
    data object NativeHome : TvTabDestination

    data class Web(val url: String) : TvTabDestination
}

internal data class TvTabSpec(
    val title: String,
    val widthWeight: Float,
    val destination: TvTabDestination,
    val recommended: Boolean = false
)

internal val TV_TABS = listOf(
    TvTabSpec("首页", 0.75f, TvTabDestination.NativeHome),
    TvTabSpec(
        "有事找党员",
        1.10f,
        TvTabDestination.Web(TEMPORARY_WEB_URL),
        recommended = true
    ),
    TvTabSpec("咔哒时间·康巴党旗红", 1.65f, TvTabDestination.Web(TEMPORARY_WEB_URL)),
    TvTabSpec("师资库", 0.80f, TvTabDestination.Web(TEMPORARY_WEB_URL)),
    TvTabSpec("阵地库", 0.80f, TvTabDestination.Web(TEMPORARY_WEB_URL)),
    TvTabSpec("课件库", 0.80f, TvTabDestination.Web(TEMPORARY_WEB_URL)),
    TvTabSpec("我的党支部", 1.10f, TvTabDestination.Web(TEMPORARY_WEB_URL))
)
