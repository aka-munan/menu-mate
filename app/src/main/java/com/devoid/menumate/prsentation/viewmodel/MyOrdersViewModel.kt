package com.devoid.menumate.prsentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.devoid.menumate.data.remote.TableManager
import com.devoid.menumate.data.remote.TableState
import com.devoid.menumate.domain.model.KitchenOrder
import com.devoid.menumate.domain.model.ORDERSTATUS_READY
import com.devoid.menumate.domain.model.RemoteOrder
import com.devoid.menumate.domain.repository.KitchenOrdersRepository
import com.devoid.menumate.prsentation.state.OrderState
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MyOrdersViewModel @Inject constructor(
    private val kitchenOrdersRepository: KitchenOrdersRepository,
    private val tableManager: TableManager
) :
    ViewModel() {
    private val tableInfo = (tableManager.tableState.value as TableState.Initialized).tableInfo
    private val TAG = this::class.simpleName
    private val currentUser = Firebase.auth.currentUser!!
    private val _orderState = MutableStateFlow<OrderState>(OrderState.Loading)
    val orderState = _orderState.asStateFlow()

    init {
        observeOrders()
    }

    fun observeOrders() {
        _orderState.value = OrderState.Loading
        kitchenOrdersRepository.isInitialLoad = true
        kitchenOrdersRepository.observeMyOrders(
            currentUser.uid, tableInfo.tableNumber,
            onLoad = { orders ->
                _orderState.value = OrderState.Success(orders)
            },
            onModify = { remoteOrder, id ->
                _orderState.update {
                    val state = it as OrderState.Success<KitchenOrder>

                    OrderState.Success(state.orders.toMutableList().apply {
                        val index = indexOfFirst { it.id == id }
                        if (index != -1)
                            this[index] = remoteOrder
                    })
                }
            },
            onRemove = { id: String ->
                _orderState.update { orderState ->
                    val state = orderState as OrderState.Success<RemoteOrder>
                    val index=orderState.orders.indexOfFirst { it.id == id }
                    OrderState.Success(
                        state.orders.toMutableList().apply {
                           this[index]= this[index].copy(status = ORDERSTATUS_READY)
                        }
                    )
                }
            },
            onError = { e ->
                _orderState.value = OrderState.Error(e)
                Log.e(TAG, "observeOrders: ", e)
            }
        )
    }
}