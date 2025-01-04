package com.devoid.menumate.domain.model

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.devoid.menumate.data.remote.RESTAURANT_ID
import com.devoid.menumate.data.remote.SERVER_ADDR
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import okhttp3.HttpUrl

@Entity(tableName = "likedItems")
data class LikedItemInstance (
    @PrimaryKey
    val itemId: String,
    val resId: String,
    val name : String,
    val description: String,
    val price : Int
){
    val imageUrl: Uri
        get() {
            return HttpUrl.Builder()
                .scheme("http")
                .host(SERVER_ADDR)
                .port(3000)
                .addPathSegments("download")
                .addQueryParameter("image", itemId)
                .addQueryParameter("uid",Firebase.auth.uid)
                .build().toString().toUri()
        }
}
fun MenuItem.toLikedItemInstance() : LikedItemInstance {
    return LikedItemInstance(itemId = this.itemId, resId = RESTAURANT_ID, name = this.itemName,description = this.description, price= this.price)
}