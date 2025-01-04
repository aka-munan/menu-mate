package com.devoid.menumate.domain.model

import android.net.Uri
import androidx.core.net.toUri
import com.devoid.menumate.data.remote.SERVER_ADDR
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import okhttp3.HttpUrl
@IgnoreExtraProperties
data class MenuItem(
    var itemName: String = "",
    var time: String = "",
    var description: String = "",
    var category: String = "",
    var itemId: String = "",
    var price: Int = 0,
    var rating: Float = 5.0f,
    @get:Exclude
    val isRemote:Boolean=true
    ) {
    @get:Exclude
    var imageUrl: Uri = "".toUri()
        get() {
            return if (isRemote)//if remote return url to server
                HttpUrl.Builder()
                    .scheme("http")
                    .host(SERVER_ADDR)
                    .port(3000)
                    .addPathSegments("download")
                    .addQueryParameter("image", itemId)
                    .addQueryParameter("uid", Firebase.auth.uid)
                    .build().toString().toUri()
            else
                field
        }
}
