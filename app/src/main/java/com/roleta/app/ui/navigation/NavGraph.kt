package com.roleta.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.roleta.app.ui.screen.history.HistoryScreen
import com.roleta.app.ui.screen.home.HomeScreen
import com.roleta.app.ui.screen.list.ListScreen
import com.roleta.app.ui.screen.pick.PickScreen

@Composable
fun RoletaNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = HomeRoute) {

        composable<HomeRoute> {
            HomeScreen(
                onNavigateToList = { listId, listName ->
                    navController.navigate(ListRoute(listId, listName))
                }
            )
        }

        composable<ListRoute> { backStackEntry ->
            val route: ListRoute = backStackEntry.toRoute()
            ListScreen(
                listId = route.listId,
                listName = route.listName,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPick = { listId, listName ->
                    navController.navigate(PickRoute(listId, listName))
                },
                onNavigateToHistory = { listId, listName ->
                    navController.navigate(HistoryRoute(listId, listName))
                }
            )
        }

        composable<PickRoute> { backStackEntry ->
            val route: PickRoute = backStackEntry.toRoute()
            PickScreen(
                listId = route.listId,
                listName = route.listName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<HistoryRoute> { backStackEntry ->
            val route: HistoryRoute = backStackEntry.toRoute()
            HistoryScreen(
                listId = route.listId,
                listName = route.listName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
