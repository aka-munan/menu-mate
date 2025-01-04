package com.devoid.menumate.data.reository

import com.devoid.menumate.data.roomsql.LikedItemsDAO
import com.devoid.menumate.data.roomsql.OrderDAO
import com.devoid.menumate.domain.model.LikedItemInstance
import com.devoid.menumate.domain.model.Order
import com.devoid.menumate.domain.repository.LocalDatabaseRepository
import javax.inject.Inject

class LocalDatabaseRepositoryImpl @Inject constructor(
    private val orderDAO: OrderDAO,
    private val likedItemsDAO: LikedItemsDAO
) : LocalDatabaseRepository {
    override suspend fun getAllFromCart(): List<Order> {
        return orderDAO.getAllItemsFromCart()
    }

    override suspend fun getByIDFromCart(id: String): Order {
        return orderDAO.getByID(id)
    }

    override suspend fun addToCart(order: Order) {
        orderDAO.addToCart(order)
    }

    override suspend fun deleteFromCart(order: Order) {
        orderDAO.deleteFromCart(order)
    }

    override suspend fun deleteAllOrders() {
        orderDAO.deleteAll()
    }

    override suspend fun getLikedItems(restaurantID: String): List<LikedItemInstance> {
      return likedItemsDAO.getAllByResId(restaurantID)
    }

    override suspend fun getLikedItemByID(id: String): LikedItemInstance {
        return likedItemsDAO.getByID(id)
    }

    override suspend fun addToLikedItems(likedItemInstance: LikedItemInstance) {
        likedItemsDAO.addToLikedItems(likedItemInstance)
    }

    override suspend fun deleteLikedItem(likedItemInstance: LikedItemInstance) {
        likedItemsDAO.delete(likedItemInstance)
    }

    override suspend fun deleteLikedItemByID(id: String) {
        likedItemsDAO.delete(id)
    }


}