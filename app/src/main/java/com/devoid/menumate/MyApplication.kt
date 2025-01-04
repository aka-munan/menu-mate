package com.devoid.menumate

import android.app.Application
import com.devoid.menumate.domain.model.TableInfo
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltAndroidApp
class MyApplication:Application() {

}