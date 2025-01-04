package com.devoid.menumate.domain.repository

import com.devoid.menumate.domain.model.KitchenOrder
import com.devoid.menumate.domain.model.RemoteOrder

interface KitchenOrdersRepository {
    var isInitialLoad: Boolean
    fun observeOrders(
        onAdd: (KitchenOrder) -> Unit,
        onModify: (KitchenOrder, id: String) -> Unit,
        onRemove: (id: String) -> Unit,
        onInitialLoad: (List<KitchenOrder>) -> Unit,
        onError: (Exception) -> Unit
    )

    fun updateOrder(order: KitchenOrder, onComplete: (Exception?) -> Unit)
    fun observeMyOrders(
        uid: String, tableNumber: Int,
        onLoad: (List<KitchenOrder?>) -> Unit,
        onModify: (KitchenOrder, id: String) -> Unit,
        onRemove: (id: String) -> Unit,
        onError: (Exception) -> Unit
    )
}