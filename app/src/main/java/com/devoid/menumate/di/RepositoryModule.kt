package com.devoid.menumate.di

import android.content.Context
import androidx.room.Room
import com.devoid.menumate.data.remote.TableManager
import com.devoid.menumate.data.reository.KitchenOrdersRepositoryImpl
import com.devoid.menumate.data.reository.LocalDatabaseRepositoryImpl
import com.devoid.menumate.data.reository.LoginRepositoryImpl
import com.devoid.menumate.data.reository.RestaurantRepositoryImpl
import com.devoid.menumate.data.roomsql.AppDatabase
import com.devoid.menumate.data.roomsql.LikedItemsDAO
import com.devoid.menumate.data.roomsql.OrderDAO
import com.devoid.menumate.domain.repository.KitchenOrdersRepository
import com.devoid.menumate.domain.repository.LocalDatabaseRepository
import com.devoid.menumate.domain.repository.LoginRepository
import com.devoid.menumate.domain.repository.RestaurantRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindKitchenOrdersRepository(
        kitchenOrdersRepositoryImpl: KitchenOrdersRepositoryImpl
    ): KitchenOrdersRepository

    @Binds
    abstract fun bindRestaurantRepository(restaurantRepositoryImpl: RestaurantRepositoryImpl): RestaurantRepository
    @Binds
    abstract fun bindLocalDatabaseRepository(localDatabaseRepositoryImpl: LocalDatabaseRepositoryImpl):LocalDatabaseRepository
    @Binds
    abstract fun bindLoginRepository(loginRepositoryImpl: LoginRepositoryImpl):LoginRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideTableManager(): TableManager = TableManager()

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context):AppDatabase{
        return Room.databaseBuilder(context, AppDatabase::class.java, "local-database")
            .build()
    }
    @Singleton
    @Provides
    fun provideOrdersDAO(appDatabase: AppDatabase):OrderDAO{
        return appDatabase.orderDao()
    }
    @Singleton
    @Provides
    fun provideLikedItemsDAO(appDatabase: AppDatabase):LikedItemsDAO{
        return appDatabase.likedItemsDao()
    }
}