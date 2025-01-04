package com.devoid.menumate.domain.repository

import com.devoid.menumate.domain.model.LikedItemInstance
import com.devoid.menumate.domain.model.Order

interface LocalDatabaseRepository {
    suspend fun getAllFromCart():List<Order>
    suspend fun getByIDFromCart(id:String):Order?
    suspend fun addToCart(order: Order)
    suspend fun deleteFromCart(order: Order)
    suspend fun deleteAllOrders()
    suspend fun getLikedItems(restaurantID:String):List<LikedItemInstance>
    suspend fun getLikedItemByID(id:String):LikedItemInstance?
    suspend fun addToLikedItems(likedItemInstance: LikedItemInstance)
    suspend fun deleteLikedItem(likedItemInstance: LikedItemInstance)
    suspend fun deleteLikedItemByID(id: String)
}