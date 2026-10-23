package com.example.s25app.logic

import com.example.s25app.model.AppData
import com.example.s25app.model.MealType
import com.example.s25app.model.Nutrition
import com.example.s25app.model.Recipe
import com.example.s25app.model.planKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryTest {
    private val owsianka = Recipe(1, "Owsianka", MealType.BREAKFAST, nutrition = Nutrition(kcal = 400.0))
    private val jajecznica = Recipe(2, "Jajecznica", MealType.BREAKFAST, nutrition = Nutrition(kcal = 300.0))
    private val zupa = Recipe(3, "Zupa", MealType.LUNCH, nutrition = Nutrition(kcal = 500.0))
    private val mon = planKey(0, MealType.BREAKFAST)
    private val tue = planKey(1, MealType.BREAKFAST)
    private val wed = planKey(2, MealType.BREAKFAST)
    private val monLunch = planKey(0, MealType.LUNCH)
    private val data = AppData(
        recipes = listOf(owsianka, jajecznica, zupa),
        plan = mapOf(mon to 1L, tue to 1L, wed to 2L, monLunch to 3L),
        nextId = 10,
    )

    @Test
    fun toggleAddsAndRemovesHistoryEntry() {
        val eaten = Diary.toggleEaten(data, mon, "2026-10-20")
        assertEquals(1, eaten.history.size)
        assertEquals("Owsianka", eaten.history[0].name)
        assertTrue(mon in eaten.eaten)

        val undone = Diary.toggleEaten(eaten, mon, "2026-10-20")
        assertEquals(0, undone.history.size)
        assertEquals(0, undone.eaten.size)
    }

    @Test
    fun countsMealsPerCategoryAndRecipe() {
        var d = data
        d = Diary.toggleEaten(d, mon, "2026-10-20")
        d = Diary.toggleEaten(d, tue, "2026-10-21")
        d = Diary.toggleEaten(d, wed, "2026-10-22")
        d = Diary.toggleEaten(d, monLunch, "2026-10-20")

        val stats = Diary.stats(d.history, null).associateBy { it.mealType }
        val breakfast = stats.getValue(MealType.BREAKFAST)
        assertEquals(3, breakfast.count)
        assertEquals(1100.0, breakfast.kcal, 0.001)
        assertEquals(RecipeCount("Owsianka", 2), breakfast.recipes[0])
        assertEquals(RecipeCount("Jajecznica", 1), breakfast.recipes[1])
        assertEquals(1, stats.getValue(MealType.LUNCH).count)

        assertEquals(2, Diary.stats(d.history, "2026-10-21").getValue(MealType.BREAKFAST).count)
    }

    private fun List<CategoryStats>.getValue(type: MealType) = first { it.mealType == type }

    @Test
    fun clearingPlanKeepsHistory() {
        val eaten = Diary.toggleEaten(data, mon, "2026-10-20")
        val cleared = Diary.clearPlan(eaten)
        assertEquals(0, cleared.plan.size)
        assertEquals(0, cleared.eaten.size)
        assertEquals(1, cleared.history.size)
    }

    @Test
    fun changingSlotRecipeUndoesEaten() {
        val eaten = Diary.toggleEaten(data, mon, "2026-10-20")
        val changed = Diary.setSlot(eaten, mon, 2L)
        assertEquals(2L, changed.plan[mon])
        assertEquals(0, changed.history.size)
        assertEquals(0, changed.eaten.size)
    }
}
