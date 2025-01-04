package com.devoid.menumate.prsentation.viewmodel

import androidx.lifecycle.ViewModel
import com.devoid.menumate.data.remote.TableManager
import com.devoid.menumate.data.remote.TableState
import com.devoid.menumate.domain.model.KitchenOrder
import com.devoid.menumate.domain.model.LikedItemInstance
import com.devoid.menumate.domain.model.MenuItem
import com.devoid.menumate.domain.model.ORDERSTATUS_PENDING
import com.devoid.menumate.domain.model.Order
import com.devoid.menumate.domain.model.RemoteOrder
import com.devoid.menumate.domain.repository.KitchenOrdersRepository
import com.devoid.menumate.domain.repository.LocalDatabaseRepository
import com.devoid.menumate.prsentation.state.OrderState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CheckOutFragmentSharedViewModel @Inject constructor(
    private val localDatabaseRepository: LocalDatabaseRepository,
    private val kitchenOrdersRepository: KitchenOrdersRepository,
    private val tableManager: TableManager
) : ViewModel() {
    private val _cartItems = MutableStateFlow<OrderState>(OrderState.Loading)
    val cartItems = _cartItems.asStateFlow()
    private val currentUser = Firebase.auth.currentUser!!
    private val _likedItems = MutableStateFlow<List<LikedItemInstance>>(emptyList())
    val likedItems = _likedItems.asStateFlow()


    suspend fun getLikedItems(){
        val tableInfo = when (val tableState = tableManager.tableState.value) {
            is TableState.Initialized -> {
                tableState.tableInfo
            }

            else -> throw IllegalStateException("Table manager not initialized")
        }
        _likedItems.value= localDatabaseRepository.getLikedItems(tableInfo.restaurantId)
    }

    suspend fun getLikedItem(id: String): LikedItemInstance? {
        return localDatabaseRepository.getLikedItemByID(id)
    }

    suspend fun addToLikedItems(likedItemInstance: LikedItemInstance) {
        localDatabaseRepository.addToLikedItems(likedItemInstance)
    }

    suspend fun deleteLikedItem(id: String) {
        localDatabaseRepository.deleteLikedItemByID(id)
    }

    suspend fun getCartItems() {
        _cartItems.value = OrderState.Success(localDatabaseRepository.getAllFromCart())
    }

    suspend fun addToCart(order: Order) {
        localDatabaseRepository.addToCart(order)
    }

    suspend fun deleteFromCart(order: Order) {
        localDatabaseRepository.deleteFromCart(order)
    }

    suspend fun clearCart() {
        localDatabaseRepository.getAllFromCart()
    }

    fun performOrder(
        menuItem: MenuItem,
        spi: String,
        itemCount: Int,
        onResult: (Exception?) -> Unit
    ) {
        val tableInfo = when (val tableState = tableManager.tableState.value) {
            is TableState.Initialized -> {
                tableState.tableInfo
            }

            else -> throw IllegalStateException("Table manager not initialized")
        }
        val remoteOrder = RemoteOrder(menuItem.itemId, menuItem.itemName, itemCount)
        val order = KitchenOrder(
            null, spi, tableInfo.tableNumber,
            ORDERSTATUS_PENDING, currentUser.uid, "", listOf(remoteOrder)
        )
        kitchenOrdersRepository.updateOrder(order, onResult)
    }

    fun performOrder(orders: List<Order>, spi: String, onResult: (Exception?) -> Unit) {
        val tableInfo = when (val tableState = tableManager.tableState.value) {
            is TableState.Initialized -> {
                tableState.tableInfo
            }

            else -> throw IllegalStateException("Table manager not initialized")
        }
        val remoteOrders = mutableListOf<RemoteOrder>()
        orders.forEach { order ->
            remoteOrders.add(RemoteOrder(order.itemId, order.name, order.itemCount))
        }
        val order = KitchenOrder(
            null, spi, tableInfo.tableNumber,
            ORDERSTATUS_PENDING, currentUser.uid, "", remoteOrders
        )
        kitchenOrdersRepository.updateOrder(order, onResult)
    }

}