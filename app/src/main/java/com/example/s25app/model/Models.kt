package com.example.s25app.model

import kotlinx.serialization.Serializable

@Serializable
enum class MealType(val label: String) {
    BREAKFAST("Śniadanie"),
    SNACK("Przekąska"),
    LUNCH("Obiad"),
    DINNER("Kolacja"),
}

@Serializable
data class Ingredient(
    val name: String,
    val amount: Double,
    val unit: String = "g",
    val householdQty: Double? = null,
    val householdUnit: String = "",
)

@Serializable
data class Nutrition(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val fiber: Double = 0.0,
    val ww: Double = 0.0,
) {
    operator fun plus(o: Nutrition) = Nutrition(
        kcal + o.kcal, protein + o.protein, fat + o.fat,
        carbs + o.carbs, fiber + o.fiber, ww + o.ww,
    )
}

@Serializable
data class Recipe(
    val id: Long = 0,
    val name: String,
    val mealType: MealType,
    val ingredients: List<Ingredient> = emptyList(),
    val nutrition: Nutrition = Nutrition(),
    val steps: String = "",
)

/** Klucz slotu w planie: "<dzień 0-6>-<MealType>". */
fun planKey(day: Int, meal: MealType) = "$day-${meal.name}"

@Serializable
data class AppData(
    val recipes: List<Recipe> = emptyList(),
    /** slot planu -> id przepisu */
    val plan: Map<String, Long> = emptyMap(),
    /** klucz składnika -> ilość w domu (w jednostce przepisu) */
    val pantry: Map<String, Double> = emptyMap(),
    /** klucz składnika -> wielkość opakowania w sklepie */
    val packageSizes: Map<String, Double> = emptyMap(),
    /** klucze składników odhaczonych na liście zakupów */
    val bought: Set<String> = emptySet(),
    val nextId: Long = 1,
)

/** Klucz pozwalający łączyć ten sam składnik z różnych przepisów. */
fun ingredientKey(name: String, unit: String) =
    name.trim().lowercase().replace(Regex("\\s+"), " ") + "|" + unit.trim().lowercase()
