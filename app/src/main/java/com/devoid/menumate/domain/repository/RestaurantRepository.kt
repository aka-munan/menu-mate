package com.devoid.menumate.domain.repository

import com.devoid.menumate.domain.model.MenuItem
import com.devoid.menumate.domain.model.Restaurant
import com.google.android.gms.tasks.Task

interface RestaurantRepository{
    fun getRemoteRestaurant(restaurantId: String, onResult: (Restaurant?) -> Unit)
    fun loadMenuItems(restaurantId: String, onResult: (List<MenuItem>) -> Unit)
    fun loadMenuItemsByCategory(
        restaurantId: String,
        category: String,
        onResult: (e: Exception?, List<MenuItem>) -> Unit
    )

    fun loadMenuItemsByRating(
        restaurantId: String,
        onResult: (e: Exception?, List<MenuItem>) -> Unit
    )

    fun loadMenuItemsByName(
        restaurantId: String,
        name:String,
        onResult: (e: Exception?, List<MenuItem>) -> Unit
    )

    fun saveToRemoteRestaurant(
        restaurantId: String,
        restaurant: Restaurant,
        onResult: (Exception?) -> Unit
    )

    fun addMenuItems(
        restaurantId: String,
        menuItems: List<MenuItem>,
        onResult: (Exception?) -> Unit
    ): Task<Void>

    fun removeMenuItems(
        restaurantId: String,
        menuItems: List<MenuItem>,
        onResult: (Exception?) -> Unit
    ): Task<Void>

    suspend fun uploadThumbnailsToBlob(restaurantId: String, menuItems: List<MenuItem>)
    suspend fun deleteThumbnailsFromBlob(restaurantId: String, menuItems: List<MenuItem>)
}