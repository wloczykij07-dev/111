package com.example.s25app.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDataTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun readsDataSavedBeforeFavoritesExisted() {
        val old = """{"recipes":[{"id":1,"name":"Owsianka","mealType":"BREAKFAST"}],"nextId":2}"""
        val data = json.decodeFromString<AppData>(old)
        assertEquals("Owsianka", data.recipes.single().name)
        assertFalse(data.recipes.single().favorite)
    }

    @Test
    fun keepsFavoriteFlag() {
        val data = AppData(recipes = listOf(Recipe(id = 1, name = "Owsianka", mealType = MealType.BREAKFAST, favorite = true)))
        val restored = json.decodeFromString<AppData>(json.encodeToString(AppData.serializer(), data))
        assertTrue(restored.recipes.single().favorite)
    }
}
