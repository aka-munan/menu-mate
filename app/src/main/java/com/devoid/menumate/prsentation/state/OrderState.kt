package com.devoid.menumate.prsentation.state


sealed interface OrderState {
    data object Loading: OrderState
    data class Success<T>(val orders: List<T>) : OrderState
    data class Error(val exception: Exception) : OrderState
}