package com.eurojackpotai.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Casino
import androidx.compose.ui.graphics.vector.ImageVector
import com.eurojackpotai.app.R

enum class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home),
    GENERATE("generate", R.string.nav_generate, Icons.Filled.Casino),
    STATISTICS("statistics", R.string.nav_statistics, Icons.Filled.BarChart),
    MY_NUMBERS("my_numbers", R.string.nav_my_numbers, Icons.Filled.FormatListBulleted),
    RESULTS("results", R.string.nav_results, Icons.Filled.EmojiEvents)
}
