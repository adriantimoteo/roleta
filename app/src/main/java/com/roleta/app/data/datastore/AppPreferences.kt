package com.roleta.app.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class SortOrder { ALPHA, CREATION }

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_HAS_LAUNCHED = booleanPreferencesKey("has_launched")
        private val KEY_LIST_SORT = stringPreferencesKey("list_sort")
        private val KEY_ITEM_SORT = stringPreferencesKey("item_sort")
    }

    val hasLaunched: Flow<Boolean> = dataStore.data.map { it[KEY_HAS_LAUNCHED] ?: false }

    val listSortOrder: Flow<SortOrder> = dataStore.data.map {
        SortOrder.valueOf(it[KEY_LIST_SORT] ?: SortOrder.ALPHA.name)
    }

    val itemSortOrder: Flow<SortOrder> = dataStore.data.map {
        SortOrder.valueOf(it[KEY_ITEM_SORT] ?: SortOrder.ALPHA.name)
    }

    suspend fun setHasLaunched() {
        dataStore.edit { it[KEY_HAS_LAUNCHED] = true }
    }

    suspend fun setListSortOrder(sort: SortOrder) {
        dataStore.edit { it[KEY_LIST_SORT] = sort.name }
    }

    suspend fun setItemSortOrder(sort: SortOrder) {
        dataStore.edit { it[KEY_ITEM_SORT] = sort.name }
    }
}
