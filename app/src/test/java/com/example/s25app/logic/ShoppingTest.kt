package com.example.s25app.logic

import com.example.s25app.model.AppData
import com.example.s25app.model.Ingredient
import com.example.s25app.model.MealType
import com.example.s25app.model.Recipe
import com.example.s25app.model.ingredientKey
import com.example.s25app.model.planKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ShoppingTest {
    private val owsianka = Recipe(
        id = 1, name = "Owsianka", mealType = MealType.BREAKFAST,
        ingredients = listOf(
            Ingredient("Płatki owsiane", 40.0, "g", 4.0, "Łyżka"),
            Ingredient("Jogurt naturalny", 150.0, "g"),
        ),
    )
    private val koktajl = Recipe(
        id = 2, name = "Koktajl", mealType = MealType.SNACK,
        ingredients = listOf(Ingredient("jogurt naturalny ", 100.0, "g")),
    )
    private val jogurt = ingredientKey("Jogurt naturalny", "g")

    private val data = AppData(
        recipes = listOf(owsianka, koktajl),
        plan = mapOf(
            planKey(0, MealType.BREAKFAST) to 1L,
            planKey(1, MealType.BREAKFAST) to 1L,
            planKey(0, MealType.SNACK) to 2L,
        ),
    )

    @Test
    fun sumsSameIngredientAcrossRecipes() {
        val items = Shopping.compute(data).associateBy { it.key }
        assertEquals(400.0, items.getValue(jogurt).needed, 0.001)
        assertEquals(80.0, items.getValue(ingredientKey("Płatki owsiane", "g")).toBuy, 0.001)
        assertEquals(8.0, items.getValue(ingredientKey("Płatki owsiane", "g")).householdQty!!, 0.001)
    }

    @Test
    fun subtractsPantryAndRoundsToPackages() {
        val d = data.copy(pantry = mapOf(jogurt to 50.0), packageSizes = mapOf(jogurt to 400.0))
        val item = Shopping.compute(d).first { it.key == jogurt }
        assertEquals(350.0, item.missing, 0.001)
        assertEquals(1, item.packages)
        assertEquals(400.0, item.toBuy, 0.001)
        assertEquals(50.0, item.leftover, 0.001)
    }

    @Test
    fun closeWeekMovesLeftoversToPantry() {
        val d = data.copy(packageSizes = mapOf(jogurt to 500.0), bought = setOf(jogurt))
        val after = Shopping.closeWeek(d)
        assertEquals(100.0, after.pantry.getValue(jogurt), 0.001)
        // Płatki nie zostały odhaczone, więc nic z nich nie zostało.
        assertFalse(after.pantry.containsKey(ingredientKey("Płatki owsiane", "g")))
        assertEquals(0, after.plan.size)
        assertEquals(0, after.bought.size)
    }
}
