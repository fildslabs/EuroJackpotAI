package com.eurojackpotai.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eurojackpotai.app.presentation.generate.GenerateScreen
import com.eurojackpotai.app.presentation.home.HomeScreen
import com.eurojackpotai.app.presentation.mynumbers.MyNumbersScreen
import com.eurojackpotai.app.presentation.results.ResultsScreen
import com.eurojackpotai.app.presentation.statistics.StatisticsScreen

@Composable
fun EuroJackpotNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { EuroJackpotBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.HOME.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.HOME.route) {
                HomeScreen(onNavigateToTab = { navController.navigateToBottomTab(it) })
            }
            composable(BottomNavItem.GENERATE.route) { GenerateScreen() }
            composable(BottomNavItem.STATISTICS.route) { StatisticsScreen() }
            composable(BottomNavItem.MY_NUMBERS.route) { MyNumbersScreen() }
            composable(BottomNavItem.RESULTS.route) { ResultsScreen() }
        }
    }
}

@Composable
private fun EuroJackpotBottomBar(navController: androidx.navigation.NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    NavigationBar {
        BottomNavItem.entries.forEach { item ->
            val selected = currentRoute?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToBottomTab(item) },
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                label = { Text(stringResource(item.labelRes)) }
            )
        }
    }
}

private fun androidx.navigation.NavHostController.navigateToBottomTab(item: BottomNavItem) {
    navigate(item.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
