package com.devoid.menumate.data.roomsql

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devoid.menumate.domain.model.LikedItemInstance
import com.devoid.menumate.domain.model.Order

@Dao
interface OrderDAO {
    @Query("SELECT * FROM cart")
    fun getAllItemsFromCart():List<Order>

    @Query("SELECT * FROM cart WHERE itemId = :itemId")
    fun getByID(itemId:String): Order

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addToCart(vararg order: Order)

    @Delete
    fun deleteFromCart(vararg order: Order)

    @Query("DELETE FROM cart")
    fun deleteAll()
}

@Dao
interface LikedItemsDAO{
    @Query("SELECT * FROM likedItems WHERE resId = :resId")
    fun getAllByResId(resId: String):List<LikedItemInstance>

    @Query("SELECT * FROM likedItems WHERE itemId = :itemId")
    fun getByID(itemId:String): LikedItemInstance

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addToLikedItems(vararg instance : LikedItemInstance)

    @Delete()
    fun delete(vararg order: LikedItemInstance)

    @Query("DELETE FROM likedItems WHERE itemId = :itemId")
    fun delete(itemId: String)
}