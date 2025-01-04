package com.devoid.menumate.prsentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.devoid.menumate.data.reository.KitchenOrdersRepositoryImpl
import com.devoid.menumate.domain.repository.KitchenOrdersRepository
import com.devoid.menumate.domain.model.KitchenOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class KitchenDashboardViewModel @Inject constructor(
    private val kitchenOrdersRepository: KitchenOrdersRepository
):ViewModel() {
    private val TAG = KitchenDashboardViewModel::class.simpleName
    private val _orders = MutableStateFlow<List<KitchenOrder>>(emptyList())
    val orders = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadOrders()
    }
    fun loadOrders() {
        kitchenOrdersRepository.isInitialLoad= true
        _isLoading.value = true
        kitchenOrdersRepository.observeOrders(
            onAdd = { order ->
                _orders.value += order
            },
            onModify = { order, id ->
                _orders.update { orders->
                    orders.toMutableList().apply {
                        val index = indexOfFirst { it.id == id }
                        if (index!=-1)
                            this[index] = order
                    }

                }
                Log.i(TAG, "loadOrders: modified $id")
            },
            onRemove = { id ->
                _orders.value = _orders.value.filterNot { it.id == id }
            },
            onInitialLoad = { orders ->
                _orders.value = orders
                _isLoading.value = false
            },
            onError = {error->
                _isLoading.value = false
            }
        )
    }
    fun updateOrder(order: KitchenOrder){
        kitchenOrdersRepository.updateOrder(order){e->
            e?.let { 
                //failed
                Log.e(TAG, "updateOrder: failed to update database, error:\n ", e)
            }
        }
    }
}