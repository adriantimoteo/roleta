package com.roleta.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class ListRoute(val listId: String, val listName: String)

@Serializable
data class PickRoute(val listId: String, val listName: String)

@Serializable
data class HistoryRoute(val listId: String, val listName: String)
