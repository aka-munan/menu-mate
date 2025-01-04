package com.devoid.menumate.domain.model

import com.google.firebase.firestore.Exclude

data class Restaurant(
    var name: String="",
    var desc: String="",
    var cats: List<String> = emptyList(),
    @Exclude
    val menu_items: List<MenuItem>? = emptyList()
)
