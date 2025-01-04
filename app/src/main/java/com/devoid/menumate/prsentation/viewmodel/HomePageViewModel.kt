package com.devoid.menumate.prsentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoid.menumate.data.remote.TableManager
import com.devoid.menumate.data.remote.TableState
import com.devoid.menumate.domain.model.MenuItem
import com.devoid.menumate.domain.model.Order
import com.devoid.menumate.domain.model.Restaurant
import com.devoid.menumate.domain.model.TableInfo
import com.devoid.menumate.domain.repository.LocalDatabaseRepository
import com.devoid.menumate.domain.repository.RestaurantRepository
import com.devoid.menumate.prsentation.state.RestaurantUiState
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomePageViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val localDatabaseRepository: LocalDatabaseRepository,
    private val tableManager: Lazy<TableManager>
) :
    ViewModel() {

    var itemsLoaded = false
    var tableInfo: TableInfo
    private val _restaurantUIState = MutableStateFlow<RestaurantUiState>(RestaurantUiState.Loading)
    val state = _restaurantUIState.asStateFlow()
    private val _itemsByRating = MutableStateFlow<List<MenuItem>>(emptyList())
    val itemsByRating = _itemsByRating.asStateFlow()
    private val _query = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: Flow<List<MenuItem>> =
        _query.asStateFlow().debounce(400).flatMapLatest { query ->
            searchMenuItemsByName(query)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        tableInfo = (tableManager.get().tableState.value as TableState.Initialized).tableInfo
        getRestaurant()
    }

    private fun getRestaurant() {
        restaurantRepository.getRemoteRestaurant(tableInfo.restaurantId) { restaurant ->
            restaurant?.let {
                _restaurantUIState.value = RestaurantUiState.RemoteRestaurant(restaurant)
                return@getRemoteRestaurant
            }
            _restaurantUIState.value = RestaurantUiState.LocalRestaurant(Restaurant())
        }
    }

    fun loadMenuItems(restaurant: Restaurant) {
        _restaurantUIState.value = RestaurantUiState.Loading
        restaurantRepository.loadMenuItems(tableInfo.restaurantId) { menuItems ->
            itemsLoaded = true
            _restaurantUIState.value =
                RestaurantUiState.RemoteRestaurant(restaurant.copy(menu_items = menuItems.toMutableList()))
        }
    }

    fun loadMenuItemsByRating() {
        //  _restaurantUIState.value = RestaurantUiState.Loading
        restaurantRepository.loadMenuItemsByRating(tableInfo.restaurantId) { e, menuItems ->
            _itemsByRating.value = menuItems
        }
    }

    fun loadMenuItemsByCategory(category: String) {
        val restaurant = (_restaurantUIState.value as RestaurantUiState.RemoteRestaurant).restaurant
        restaurantRepository.loadMenuItemsByCategory(
            tableInfo.restaurantId,
            category
        ) { e, menuItems ->
            _restaurantUIState.value =
                RestaurantUiState.RemoteRestaurant(restaurant.copy(menu_items = menuItems.toMutableList()))
        }
    }

    private fun searchMenuItemsByName(name: String): Flow<List<MenuItem>> = callbackFlow {
        restaurantRepository.loadMenuItemsByName(tableInfo.restaurantId, name) { e, menuItems ->
            e?.let {
                close(e)
                return@loadMenuItemsByName
            }
            trySend(menuItems).isSuccess
        }
        awaitClose { }
    }

    fun submitQuery(query: String) {
        if (query.trim().isEmpty())
            return
        _query.value = query
    }

   suspend fun addItemToCart(menuItem: MenuItem,quantity:Int) {
        val order= Order(menuItem.itemId,menuItem.itemName,quantity,menuItem.price)
        localDatabaseRepository.addToCart(order)
    }
}
