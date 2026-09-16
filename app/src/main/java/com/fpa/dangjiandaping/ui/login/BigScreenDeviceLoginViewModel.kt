package com.fpa.dangjiandaping.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fpa.dangjiandaping.data.BigScreenDeviceLoginRepository
import com.fpa.dangjiandaping.data.BigScreenDeviceSessionStore
import com.fpa.dangjiandaping.network.model.BigScreenDeviceSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** 宿主或界面发送给登录模块的唯一输入。 */
sealed interface BigScreenDeviceLoginAction {
    data object Initialize : BigScreenDeviceLoginAction

    data object Retry : BigScreenDeviceLoginAction
}

/** 可渲染的持久状态；界面只订阅此状态，不读取网络响应。 */
sealed interface BigScreenDeviceLoginState {
    data object Idle : BigScreenDeviceLoginState

    data object Loading : BigScreenDeviceLoginState

    data class Authenticated(
        val session: BigScreenDeviceSession,
    ) : BigScreenDeviceLoginState

    data class Failed(
        val message: String,
    ) : BigScreenDeviceLoginState
}

/** 登录成功/失败的单次事件，适用于日志、Toast 或导航等不可重放行为。 */
sealed interface BigScreenDeviceLoginEffect {
    data object LoginSucceeded : BigScreenDeviceLoginEffect

    data object SessionRestored : BigScreenDeviceLoginEffect

    data class LoginFailed(
        val message: String,
    ) : BigScreenDeviceLoginEffect
}

/**
 * 登录模块的单向数据流入口：
 * Action -> ViewModel -> Repository -> Retrofit -> State / Effect。
 */
class BigScreenDeviceLoginViewModel(
    private val repository: BigScreenDeviceLoginRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<BigScreenDeviceLoginState>(
        BigScreenDeviceLoginState.Idle,
    )
    private val effectChannel = Channel<BigScreenDeviceLoginEffect>(Channel.BUFFERED)
    private var loginJob: Job? = null

    val state: StateFlow<BigScreenDeviceLoginState> = mutableState.asStateFlow()
    val effects = effectChannel.receiveAsFlow()

    fun dispatch(action: BigScreenDeviceLoginAction) {
        when (action) {
            BigScreenDeviceLoginAction.Initialize -> authenticate(forceRefresh = false)
            BigScreenDeviceLoginAction.Retry -> authenticate(forceRefresh = true)
        }
    }

    private fun authenticate(forceRefresh: Boolean) {
        if (loginJob?.isActive == true) return
        loginJob = viewModelScope.launch {
            if (!forceRefresh) {
                repository.validSession()?.let { session ->
                    mutableState.value = BigScreenDeviceLoginState.Authenticated(session)
                    effectChannel.send(BigScreenDeviceLoginEffect.SessionRestored)
                    return@launch
                }
            }
            mutableState.value = BigScreenDeviceLoginState.Loading
            runCatching {
                repository.login()
            }.onSuccess { session ->
                mutableState.value = BigScreenDeviceLoginState.Authenticated(session)
                effectChannel.send(BigScreenDeviceLoginEffect.LoginSucceeded)
            }.onFailure { error ->
                val message = error.message?.takeIf { it.isNotBlank() } ?: "大屏设备登录失败。"
                mutableState.value = BigScreenDeviceLoginState.Failed(message)
                effectChannel.send(BigScreenDeviceLoginEffect.LoginFailed(message))
            }
        }
    }
}

/** 暂未引入 DI 框架时，由宿主提供登录模块的依赖。 */
class BigScreenDeviceLoginViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val repository = BigScreenDeviceLoginRepository(
        BigScreenDeviceSessionStore(context),
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BigScreenDeviceLoginViewModel::class.java)) {
            return BigScreenDeviceLoginViewModel(repository) as T
        }
        throw IllegalArgumentException("未知 ViewModel：${modelClass.name}")
    }
}
