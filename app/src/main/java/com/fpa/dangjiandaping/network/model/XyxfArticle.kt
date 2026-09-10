package com.fpa.dangjiandaping.network.model

/** 首页“组工动态”接口返回的文章摘要。 */
data class XyxfArticle(
    val id: String,
    val title: String,
    val publishedAt: String,
)

data class XyxfArticleFeed(
    val articles: List<XyxfArticle>,
    val serverMessage: String,
)
