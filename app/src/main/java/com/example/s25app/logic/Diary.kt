package com.example.s25app.logic

import com.example.s25app.model.AppData
import com.example.s25app.model.EatenMeal
import com.example.s25app.model.MealType

data class RecipeCount(val name: String, val count: Int)

data class CategoryStats(
    val mealType: MealType,
    val count: Int,
    val kcal: Double,
    val recipes: List<RecipeCount>,
)

/** Odhaczanie zjedzonych posiłków i statystyki. */
object Diary {

    /** Zaznacza lub odznacza posiłek ze slotu planu jako zjedzony w dniu [date]. */
    fun toggleEaten(data: AppData, slotKey: String, date: String): AppData {
        val entryId = data.eaten[slotKey]
        if (entryId != null) {
            return data.copy(
                eaten = data.eaten - slotKey,
                history = data.history.filterNot { it.id == entryId },
            )
        }
        val recipe = data.plan[slotKey]?.let { id -> data.recipes.firstOrNull { it.id == id } } ?: return data
        val entry = EatenMeal(
            id = data.nextId,
            recipeId = recipe.id,
            name = recipe.name,
            mealType = recipe.mealType,
            date = date,
            kcal = recipe.nutrition.kcal,
        )
        return data.copy(
            eaten = data.eaten + (slotKey to entry.id),
            history = data.history + entry,
            nextId = data.nextId + 1,
        )
    }

    /** Zmiana przepisu w slocie cofa odhaczenie, bo dotyczyło poprzedniego przepisu. */
    fun setSlot(data: AppData, slotKey: String, recipeId: Long?): AppData {
        if (data.plan[slotKey] == recipeId) return data
        val cleared = if (slotKey in data.eaten) toggleEaten(data, slotKey, "") else data
        return cleared.copy(plan = if (recipeId == null) cleared.plan - slotKey else cleared.plan + (slotKey to recipeId))
    }

    /** Czyści plan, odhaczenia i listę zakupów. Historia zjedzonych posiłków zostaje. */
    fun clearPlan(data: AppData): AppData =
        data.copy(plan = emptyMap(), eaten = emptyMap(), bought = emptySet())

    /** Statystyki zjedzonych posiłków od dnia [fromDate] (RRRR-MM-DD) lub z całej historii. */
    fun stats(history: List<EatenMeal>, fromDate: String?): List<CategoryStats> {
        val entries = history.filter { fromDate == null || it.date >= fromDate }
        return MealType.entries.map { type ->
            val ofType = entries.filter { it.mealType == type }
            CategoryStats(
                mealType = type,
                count = ofType.size,
                kcal = ofType.sumOf { it.kcal },
                recipes = ofType.groupingBy { it.name }.eachCount()
                    .map { (name, count) -> RecipeCount(name, count) }
                    .sortedWith(compareBy({ -it.count }, { it.name.lowercase() })),
            )
        }
    }
}
