package com.devoid.menumate.data.reository

import android.util.Log
import com.devoid.menumate.domain.model.KitchenOrder
import com.devoid.menumate.domain.model.ORDERSTATUS_READY
import com.devoid.menumate.domain.model.RemoteOrder
import com.devoid.menumate.domain.model.TableInfo
import com.devoid.menumate.domain.repository.KitchenOrdersRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import javax.inject.Inject

class KitchenOrdersRepositoryImpl @Inject constructor() : KitchenOrdersRepository {
    override var isInitialLoad = true
    private var ordersRef: CollectionReference
    private var tableInfo: TableInfo = TableInfo(Firebase.auth.currentUser!!.uid, 0)

    init {
        ordersRef = Firebase.firestore
            .collection("restaurants")
            .document(tableInfo.restaurantId)
            .collection("orders")

    }

    override fun observeOrders(
        onAdd: (KitchenOrder) -> Unit,
        onModify: (KitchenOrder, id: String) -> Unit,
        onRemove: (id: String) -> Unit,
        onInitialLoad: (List<KitchenOrder>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        ordersRef.whereLessThan(
            "status", ORDERSTATUS_READY)
            .orderBy("status", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.EXCLUDE) { snapshot, e ->
                e?.let {
                    onError(e)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    return@addSnapshotListener
                }
                if (isInitialLoad) {
                    isInitialLoad = false
                    val orders = snapshot.documents.map { KitchenOrder.fromFirestore(it.data) }
                    onInitialLoad(orders)
                    return@addSnapshotListener
                }

                snapshot.documentChanges.forEach { change ->
                    Log.i("tag", "${change.document.data}")
                    val order = KitchenOrder.fromFirestore(change.document.data)
                    when (change.type) {
                        DocumentChange.Type.ADDED -> onAdd(order)
                        DocumentChange.Type.MODIFIED -> onModify(order, change.document.id)
                        DocumentChange.Type.REMOVED -> onRemove(change.document.id)
                    }
                }
            }
    }

    override fun updateOrder(order: KitchenOrder, onComplete: (Exception?) -> Unit) {
       val orderDoc=when(order.id){
           ""->{
               ordersRef.document()
           }
           else->ordersRef.document(order.id)
       }
        if (order.id.isEmpty())
            order.id = orderDoc.id
           orderDoc.set(order, SetOptions.merge()).addOnCompleteListener { task ->
            onComplete(task.exception)
        }
    }

    override fun observeMyOrders(
        uid: String,
        tableNumber: Int,
        onLoad: (List<KitchenOrder?>) -> Unit,
        onModify: (KitchenOrder, id: String) -> Unit,
        onRemove: (id: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        ordersRef.whereLessThan(
            "status", ORDERSTATUS_READY)
            .whereEqualTo("uid",uid)
            .whereEqualTo("table_no",tableNumber)
            .orderBy("status", Query.Direction.DESCENDING)
            .addSnapshotListener(MetadataChanges.EXCLUDE) { snapshot, e ->
                e?.let {
                    onError(e)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    onError(EmptyDataException("Empty data received"))
                    return@addSnapshotListener
                }
                if (isInitialLoad) {
                    isInitialLoad = false
                    val orders = snapshot.documents.map { KitchenOrder.fromFirestore(it.data)}
                    onLoad(orders)
                    return@addSnapshotListener
                }

                snapshot.documentChanges.forEach { change ->
                    Log.i("tag", "${change.document.data}")
                    val order = KitchenOrder.fromFirestore(change.document.data)
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {}
                        DocumentChange.Type.MODIFIED -> onModify(order, change.document.id)
                        DocumentChange.Type.REMOVED -> onRemove(change.document.id)
                    }
                }
            }
    }

    data class EmptyDataException(override val message:String):Exception(message)

}