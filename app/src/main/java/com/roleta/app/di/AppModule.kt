package com.roleta.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.roleta.app.data.db.RoletaDatabase
import com.roleta.app.data.db.dao.ItemDao
import com.roleta.app.data.db.dao.ListDao
import com.roleta.app.data.db.dao.PickHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "roleta_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RoletaDatabase =
        Room.databaseBuilder(context, RoletaDatabase::class.java, "roleta.db")
            .addMigrations(RoletaDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideListDao(db: RoletaDatabase): ListDao = db.listDao()

    @Provides
    fun provideItemDao(db: RoletaDatabase): ItemDao = db.itemDao()

    @Provides
    fun providePickHistoryDao(db: RoletaDatabase): PickHistoryDao = db.pickHistoryDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
