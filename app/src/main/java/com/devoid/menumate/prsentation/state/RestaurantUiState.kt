package com.devoid.menumate.prsentation.state

import com.devoid.menumate.domain.model.Restaurant

sealed interface RestaurantUiState {
    data object Loading:RestaurantUiState
    data class RemoteRestaurant(val restaurant: Restaurant):RestaurantUiState
    data class LocalRestaurant(val restaurant: Restaurant): RestaurantUiState
}