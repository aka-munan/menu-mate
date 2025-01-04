package com.devoid.menumate.data.roomsql

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devoid.menumate.domain.model.LikedItemInstance
import com.devoid.menumate.domain.model.Order

@Database(entities = [Order::class, LikedItemInstance::class], version = 1)
abstract class AppDatabase:RoomDatabase() {
    abstract fun orderDao():OrderDAO
    abstract fun likedItemsDao():LikedItemsDAO
}