package com.devoid.menumate.prsentation.state

sealed interface LoginUiState {
    data object Idle:LoginUiState
    data object Success:LoginUiState
    data class Failure(val e:Exception):LoginUiState
    data object ResetPassEmailSent:LoginUiState
}