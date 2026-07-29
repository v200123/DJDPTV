package com.fpa.dangjiandaping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.tv.material3.MaterialTheme
import com.fpa.dangjiandaping.ui.adapt.ProvideScreenAdaptation
import com.fpa.dangjiandaping.ui.screen.DangJianTvScreen
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import kotlinx.coroutines.delay
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_DangJianDaPing)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
        setContent {
            MaterialTheme {
                ProvideScreenAdaptation {
                    var showSplash by rememberSaveable { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        delay(SPLASH_DURATION_MILLIS)
                        showSplash = false
                    }

                    Box(Modifier.fillMaxSize()) {
                        DangJianTvScreen()

                        AnimatedVisibility(
                            visible = showSplash,
                            enter = EnterTransition.None,
                            exit = fadeOut(animationSpec = tween(SPLASH_FADE_MILLIS)),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.startup_splash),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val SPLASH_DURATION_MILLIS = 2_000L
        const val SPLASH_FADE_MILLIS = 350
    }
}
