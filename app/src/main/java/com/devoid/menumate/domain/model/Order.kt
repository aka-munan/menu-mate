package com.devoid.menumate.domain.model

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.devoid.menumate.data.remote.SERVER_ADDR
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.firestore.Exclude
import okhttp3.HttpUrl

@IgnoreExtraProperties
@Entity(tableName = "cart")
data class Order (
    @PrimaryKey val itemId:String,
    val name:String,
    var itemCount : Int,
    val price : Int,
){
    @get:Exclude
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
    companion object{
        fun from(data :Map<String,Any>) = Order(
            itemId = data["id"] as String,
            name = data["name"] as String,
            itemCount = getInt(data["itemCount"]),
            price = getInt(data["price"])
        )
    }
}
data class RemoteOrder(
    val id : String,
    val name:String,
    var itemCount : Int,
    var status:Int=0){

    companion object{
        fun from(data :Map<String,Any>) = RemoteOrder(
            id = data["id"] as String,
            name = data["name"] as String,
            itemCount = getInt(data["itemCount"]),
            status = getInt(data["status"] ?: 0L)
        )
    }
}