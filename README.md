# DJDPTV

Android TV 党建大屏客户端，提供可同时安装的 standard 和 meishan 两种变体。

## 页面结构

- 顶部品牌、日期与导航 Tab：Jetpack Compose 原生实现。
- Tab 以下内容区：Android WebView，加载
  `http://192.168.20.233:5173/xiaoyuTv/#/teacher`。
- 支持遥控器方向键焦点、确认键点击、WebView 历史返回。
- 顶部导航使用 `androidx.tv:tv-material` 的 `TabRow` 和 `Tab`，支持图片指示器、推荐角标和焦点恢复。
- H5 可调用 `window.AndroidFocusBridge.requestPreviousTabFocus()`，将焦点交还最后聚焦的原生 Tab。
- 固定横屏、沉浸式全屏，并允许访问局域网 HTTP 地址。

## 构建

使用 JDK 17 分别构建两种 Debug APK：

```powershell
.\gradlew.bat :app:assembleStandardDebug
.\gradlew.bat :app:assembleMeishanDebug
```

- standard：`com.fpa.dangjiandaping`，输出到 `app/build/outputs/apk/standard/debug/app-standard-debug.apk`。
- meishan：`com.fpa.dangjiandaping.meishan`，输出到 `app/build/outputs/apk/meishan/debug/app-meishan-debug.apk`。

两种变体使用同一份源码和桌面名称。

电视/模拟器必须与 `192.168.20.233` 位于可互通的局域网；该地址不可达时，
原生头部仍会显示，但 WebView 内容区无法加载网页。
