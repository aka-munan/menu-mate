package com.devoid.menumate.data.reository

import android.util.Log
import androidx.core.net.toFile
import com.devoid.menumate.data.remote.RESTAURANT_ID
import com.devoid.menumate.data.remote.uploadBlob
import com.devoid.menumate.domain.model.MenuItem
import com.devoid.menumate.domain.model.Restaurant
import com.devoid.menumate.domain.repository.RestaurantRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject


class RestaurantRepositoryImpl @Inject constructor() : RestaurantRepository {
    override fun getRemoteRestaurant(restaurantId: String, onResult: (Restaurant?) -> Unit) {
        Firebase.firestore.collection("restaurants").document(restaurantId).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists() && documentSnapshot != null) {
                    onResult(documentSnapshot.toObject(Restaurant::class.java))
                } else
                    onResult(null)
            }.addOnFailureListener { e ->
                onResult(null)
                e.printStackTrace()
            }
    }

    override fun loadMenuItems(restaurantId: String, onResult: (List<MenuItem>) -> Unit) {
        Firebase.firestore.collection("restaurants").document(restaurantId).collection("menu_items")
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val menuItems =
                    documentSnapshot.documents.map { it.toObject(MenuItem::class.java)!! }
                onResult(menuItems)
            }.addOnFailureListener { e ->
                onResult(emptyList())
            }
    }

    override fun loadMenuItemsByCategory(
        restaurantId: String,
        category: String,
        onResult: (e:Exception?,List<MenuItem>) -> Unit
    ) {
        Firebase.firestore.collection("restaurants").document(RESTAURANT_ID).collection("menu_items")
            .whereEqualTo("category", category).get()
            .addOnCompleteListener { task ->
                task.exception?.let {
                    onResult(it, emptyList())
                    return@addOnCompleteListener
                }
                onResult(null,task.result.documents.map { it.toObject(MenuItem::class.java)!!})
            }
    }
    override fun loadMenuItemsByRating(
        restaurantId: String,
        onResult: (e:Exception?,List<MenuItem>) -> Unit
    ) {
        Firebase.firestore.collection("restaurants").document(RESTAURANT_ID).collection("menu_items")
            .orderBy("rating",Query.Direction.DESCENDING).get()
            .addOnCompleteListener { task ->
                task.exception?.let {
                    onResult(it, emptyList())
                    return@addOnCompleteListener
                }
                onResult(null,task.result.documents.map { it.toObject(MenuItem::class.java)!!})
            }
    }

    override fun loadMenuItemsByName(
        restaurantId: String,
        name: String,
        onResult: (e: Exception?, List<MenuItem>) -> Unit
    ) {
        Firebase.firestore.collection("restaurants").document(restaurantId).collection("menu_items")
            .whereGreaterThanOrEqualTo("itemName",name)
            .limit(10).get()
            .addOnCompleteListener { task ->
                task.exception?.let {
                    onResult(it, emptyList())
                    return@addOnCompleteListener
                }
                Log.i("TAG", "loadMenuItemsByName: ${task.result.documents}")
                onResult(null,task.result.documents.map { it.toObject(MenuItem::class.java)!!})
            }
    }

    override fun saveToRemoteRestaurant(restaurantId: String,restaurant: Restaurant, onResult: (Exception?) -> Unit) {
        Firebase.firestore.collection("restaurants").document(restaurantId).set(restaurant)
            .addOnCompleteListener { task->
                onResult(task.exception)
            }
    }

    override fun addMenuItems(
        restaurantId: String,
        menuItems: List<MenuItem>,
        onResult: (Exception?) -> Unit
    ): Task<Void> {
        return Firebase.firestore.runBatch {
            menuItems.forEach {
                Firebase.firestore.collection("restaurants").document(restaurantId)
                    .collection("menu_items").document(it.itemId).set(it)
            }
        }.addOnSuccessListener {
            onResult(null)
        }.addOnFailureListener { e ->
            onResult(e)
        }
    }

    override fun removeMenuItems(
        restaurantId: String,
        menuItems: List<MenuItem>,
        onResult: (Exception?) -> Unit
    ): Task<Void> {
        return Firebase.firestore.runBatch {
            menuItems.forEach {
                Firebase.firestore.collection("restaurants").document(restaurantId)
                    .collection("menu_items").document(it.itemId).delete()
            }
        }.addOnSuccessListener {
            onResult(null)
        }.addOnFailureListener { e ->
            onResult(e)
        }
    }

    override suspend fun uploadThumbnailsToBlob(restaurantId: String, menuItems: List<MenuItem>) =
        coroutineScope {
            val path = "restaurants/${restaurantId}/menuItems"
            menuItems.forEach { menuItem ->
                if (menuItem.imageUrl.scheme != "file") {
                    return@forEach//continue
                }
                uploadBlob(
                    menuItem.imageUrl.toFile(),
                    path,
                    "${menuItem.itemId}.jpg"
                ).addOnCompleteListener { task ->
                    task.exception?.printStackTrace()
                }
            }
        }

    override suspend fun deleteThumbnailsFromBlob(restaurantId: String, menuItems: List<MenuItem>) {

    }
}