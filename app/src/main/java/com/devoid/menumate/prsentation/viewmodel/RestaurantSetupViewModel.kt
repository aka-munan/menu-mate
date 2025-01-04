package com.devoid.menumate.prsentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.devoid.menumate.domain.model.MenuItem
import com.devoid.menumate.domain.model.Restaurant
import com.devoid.menumate.domain.repository.RestaurantRepository
import com.devoid.menumate.prsentation.state.RestaurantUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Range
import javax.inject.Inject

@HiltViewModel
class RestaurantSetupViewModel @Inject constructor(private val restaurantRepository: RestaurantRepository) :
    ViewModel() {
    private val _restaurantState = MutableStateFlow<RestaurantUiState>(RestaurantUiState.Loading)
    val restaurantUiState = _restaurantState.asStateFlow()
    var itemsLoaded = false
    private val currentUser = Firebase.auth.currentUser
    private val addedItems: MutableList<MenuItem> = mutableListOf()
    private val removedItems: MutableList<MenuItem> = mutableListOf()

    init {
        getRestaurant()
    }


    private fun getRestaurant() {
        restaurantRepository.getRemoteRestaurant(currentUser!!.uid) { restaurant ->
            restaurant?.let {
                _restaurantState.value = RestaurantUiState.RemoteRestaurant(restaurant)
                //loadMenuItems(restaurant)
                return@getRemoteRestaurant
            }
            _restaurantState.value = RestaurantUiState.LocalRestaurant(Restaurant())
        }
    }

     fun loadMenuItems(restaurant: Restaurant) {
        restaurantRepository.loadMenuItems(currentUser!!.uid) { menuItems ->
            itemsLoaded = true
            _restaurantState.value =
                RestaurantUiState.RemoteRestaurant(restaurant.copy(menu_items = menuItems.toMutableList()))
        }
    }

    fun removeMenuItem(menuItem: MenuItem) {
        val currentState = _restaurantState.value
        when (currentState) {
            is RestaurantUiState.LocalRestaurant -> {
                addedItems.remove(menuItem)
            }

            is RestaurantUiState.RemoteRestaurant -> {
                removedItems.add(menuItem)
            }

            else -> throw IllegalStateException("Invalid restaurant state")
        }
    }

    fun addMenuItem(menuItem: MenuItem) {
        addedItems.add(menuItem)
    }


    fun saveToRemoteRestaurant(
        currentMenuItems: List<MenuItem>,
        onProgress: (Exception?, @Range(from = 0, to = 100) Int) -> Unit
    ) {
        val restaurant = when (val currentState = _restaurantState.value) {
            is RestaurantUiState.LocalRestaurant -> currentState.restaurant
            is RestaurantUiState.RemoteRestaurant -> currentState.restaurant
            else -> {
                throw IllegalStateException("Invalid restaurant state")
            }
        }
        val modifiedItems = getModifiedItems(restaurant, currentMenuItems)
        addedItems.addAll(modifiedItems)
        addedItems.removeAll(removedItems)
        Log.i("TAG", "saveToRemoteRestaurant: $currentMenuItems")
        Log.i("TAG", "saveToRemoteRestaurant: ${restaurant.menu_items}")
        Log.i("TAG", "saveToRemoteRestaurant: ${addedItems}")
         onProgress(null, 30)
        restaurantRepository.saveToRemoteRestaurant(currentUser!!.uid,restaurant.copy(menu_items = null)) { e ->
            e?.let { e.printStackTrace() }
            onProgress(null, 50)
            CoroutineScope(Dispatchers.IO).launch {
                updateMenuItems{
                    CoroutineScope(Dispatchers.Main).launch{onProgress(null,70)}
                }
                uploadThumbnails(addedItems)
                CoroutineScope(Dispatchers.Main).launch{onProgress(null,100)}
            }
        }
    }

    private suspend fun updateMenuItems(onActionComplete: (Exception?) -> Unit) =
        coroutineScope {
            if (addedItems.isNotEmpty())
                restaurantRepository.addMenuItems(currentUser!!.uid, addedItems) { exception ->
                    exception?.printStackTrace()
                }.await()
            if (removedItems.isNotEmpty())
                restaurantRepository.removeMenuItems(currentUser!!.uid, removedItems) { exception ->
                    exception?.printStackTrace()
                }.await()
            onActionComplete(null)
        }

    private fun getModifiedItems(
        restaurant: Restaurant,
        currentMenuItems: List<MenuItem>
    ): List<MenuItem> {
        val modifiedItems: MutableList<MenuItem> = mutableListOf()
        restaurant.menu_items?.let {
            for (i in 0 until restaurant.menu_items.size) {
                if (restaurant.menu_items.size < i ||
                    restaurant.menu_items[i].itemId != currentMenuItems[i].itemId
                )
                    return modifiedItems

                if (restaurant.menu_items[i] != currentMenuItems[i])
                    modifiedItems.add(currentMenuItems[i])
            }
        }

        return modifiedItems
    }

    private suspend fun uploadThumbnails(menuItems: List<MenuItem>) {
        if (menuItems.isEmpty())
            return
        restaurantRepository.uploadThumbnailsToBlob(currentUser!!.uid,menuItems)
    }
}