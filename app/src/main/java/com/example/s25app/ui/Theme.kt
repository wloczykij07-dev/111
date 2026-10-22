package com.example.s25app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.s25app.model.MealType

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F0B8),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFFEF6C00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = Color(0xFF2E1500),
    tertiary = Color(0xFF8E24AA),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF7D8FF),
    onTertiaryContainer = Color(0xFF330044),
    background = Color(0xFFF6FBF0),
    surface = Color(0xFFF6FBF0),
    surfaceContainer = Color(0xFFEAF3E2),
    surfaceContainerHigh = Color(0xFFE2EEDA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD88B),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF145218),
    onPrimaryContainer = Color(0xFFB9F0B8),
    secondary = Color(0xFFFFB77C),
    onSecondary = Color(0xFF4C2700),
    secondaryContainer = Color(0xFF6C3A00),
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = Color(0xFFE5B5F5),
    onTertiary = Color(0xFF4A1060),
    tertiaryContainer = Color(0xFF632A78),
    onTertiaryContainer = Color(0xFFF7D8FF),
    background = Color(0xFF111510),
    surface = Color(0xFF111510),
    surfaceContainer = Color(0xFF1C211B),
    surfaceContainerHigh = Color(0xFF262C25),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}

/** Kolor tła i kolor akcentu dla rodzaju posiłku. */
data class MealColors(val container: Color, val accent: Color)

@Composable
fun mealColors(type: MealType): MealColors {
    val dark = isSystemInDarkTheme()
    return when (type) {
        MealType.BREAKFAST -> if (dark) MealColors(Color(0xFF4A3800), Color(0xFFFFC94D))
        else MealColors(Color(0xFFFFF1C2), Color(0xFFF2A900))
        MealType.SNACK -> if (dark) MealColors(Color(0xFF55112E), Color(0xFFFF7EB0))
        else MealColors(Color(0xFFFFDDEA), Color(0xFFE91E63))
        MealType.LUNCH -> if (dark) MealColors(Color(0xFF5A1E06), Color(0xFFFF8A5C))
        else MealColors(Color(0xFFFFE0D2), Color(0xFFF4511E))
        MealType.DINNER -> if (dark) MealColors(Color(0xFF1E2558), Color(0xFF8C9EFF))
        else MealColors(Color(0xFFE1E5FF), Color(0xFF3949AB))
    }
}

/** Kolory wartości odżywczych: kcal, białko, tłuszcz, węglowodany, błonnik, WW. */
val NutrientColors = listOf(
    Color(0xFFF57C00),
    Color(0xFFE53935),
    Color(0xFFFBC02D),
    Color(0xFF1E88E5),
    Color(0xFF43A047),
    Color(0xFF8E24AA),
)

val FavoriteRed = Color(0xFFE53935)
