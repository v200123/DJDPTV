package com.fpa.dangjiandaping.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpa.dangjiandaping.network.BigScreenDeviceLoginClient
import com.fpa.dangjiandaping.network.model.BigScreenDeviceSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BigScreenDeviceLoginUiState {
    data object Loading : BigScreenDeviceLoginUiState

    data class Authenticated(
        val session: BigScreenDeviceSession,
    ) : BigScreenDeviceLoginUiState

    data class Failed(
        val message: String,
    ) : BigScreenDeviceLoginUiState
}

/** 登录状态的唯一持有者；界面或宿主可订阅 [uiState]，不直接处理网络响应。 */
class BigScreenDeviceLoginViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableUiState = MutableStateFlow<BigScreenDeviceLoginUiState>(
        BigScreenDeviceLoginUiState.Loading,
    )
    val uiState: StateFlow<BigScreenDeviceLoginUiState> = mutableUiState.asStateFlow()

    init {
        login()
    }

    fun login() {
        mutableUiState.value = BigScreenDeviceLoginUiState.Loading
        viewModelScope.launch {
            runCatching {
                BigScreenDeviceLoginClient.login(getApplication())
            }.onSuccess { session ->
                mutableUiState.value = BigScreenDeviceLoginUiState.Authenticated(session)
            }.onFailure { error ->
                mutableUiState.value = BigScreenDeviceLoginUiState.Failed(
                    error.message?.takeIf { it.isNotBlank() } ?: "大屏设备登录失败。",
                )
            }
        }
    }
}
