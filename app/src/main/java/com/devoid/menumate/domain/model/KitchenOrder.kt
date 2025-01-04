package com.devoid.menumate.domain.model

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class KitchenOrder(
    @ServerTimestamp
    val time: Timestamp?=null,
    val spi: String,
    val table_no: Int,
    var status: Int,
    val uid: String,
    var id :String,
    val items: List<RemoteOrder>
) {
    companion object {
        fun fromFirestore(map: MutableMap<String, Any>?) = KitchenOrder(

            time = map?.get("time") as Timestamp,
            spi = map["spi"] as String,
            table_no = getInt(map["table_no"]) ,
            status = getInt(map["status"]),
            uid = map["uid"] as String,
            id = map["id"]as String,
            items = (map["items"] as? List<HashMap<String,Any>>)?.let { it.map { RemoteOrder.from(it) } } ?: emptyList()
        )
    }
}
fun getInt(value:Any?):Int{
    if(value is Int)
        return value
    if (value is Double){
        return value.toInt()
    }else if (value is Long){
        return value.toInt()
    }
    throw NumberFormatException("Invalid Number Input")
}