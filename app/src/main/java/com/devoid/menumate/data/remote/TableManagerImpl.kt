package com.devoid.menumate.data.remote

import com.devoid.menumate.domain.model.TableInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton


open class TableManager{
    private val _tableState = MutableStateFlow<TableState>(TableState.UnInitialized)
     val tableState = _tableState.asStateFlow()

     fun initialize(tableInfo: TableInfo):Boolean{
        when(_tableState.value){
            is TableState.UnInitialized->{
                _tableState.value = TableState.Initialized(tableInfo)
               return true
            }
            is TableState.Initialized->{
              return false
            }
        }
    }
}

sealed interface TableState{
    data object UnInitialized : TableState
    data class Initialized(val tableInfo: TableInfo):TableState

}