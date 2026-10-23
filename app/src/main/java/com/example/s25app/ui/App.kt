package com.example.s25app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun App(vm: AppViewModel) {
    BackHandler(enabled = vm.stack.isNotEmpty()) { vm.pop() }

    when (val screen = vm.stack.lastOrNull()) {
        is Screen.RecipeDetail -> RecipeDetailScreen(vm, screen.id)
        is Screen.RecipeEdit -> RecipeEditScreen(vm, screen)
        is Screen.Import -> ImportScreen(vm)
        is Screen.PickRecipe -> PickRecipeScreen(vm, screen)
        null -> Tabs(vm)
    }
}

@Composable
private fun Tabs(vm: AppViewModel) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = vm.tab == tab,
                        onClick = { vm.tab = tab },
                        icon = {
                            Icon(
                                when (tab) {
                                    Tab.RECIPES -> Icons.AutoMirrored.Filled.List
                                    Tab.PLAN -> Icons.Default.DateRange
                                    Tab.SHOPPING -> Icons.Default.ShoppingCart
                                    Tab.PANTRY -> Icons.Default.Home
                                    Tab.STATS -> Icons.Default.Star
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (vm.tab) {
                Tab.RECIPES -> RecipesScreen(vm)
                Tab.PLAN -> PlanScreen(vm)
                Tab.SHOPPING -> ShoppingScreen(vm)
                Tab.PANTRY -> PantryScreen(vm)
                Tab.STATS -> StatsScreen(vm)
            }
        }
    }
}
