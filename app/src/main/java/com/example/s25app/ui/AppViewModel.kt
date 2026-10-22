package com.example.s25app.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.s25app.data.Storage
import com.example.s25app.logic.RecipeParser
import com.example.s25app.logic.Shopping
import com.example.s25app.model.AppData
import com.example.s25app.model.MealType
import com.example.s25app.model.Recipe
import com.example.s25app.model.planKey
import com.example.s25app.ocr.Ocr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

enum class Tab(val label: String) {
    RECIPES("Przepisy"),
    PLAN("Plan"),
    SHOPPING("Zakupy"),
    PANTRY("Zapasy"),
}

sealed interface Screen {
    data class RecipeDetail(val id: Long) : Screen
    /** parsedIndex != null: edycja przepisu rozpoznanego w imporcie */
    data class RecipeEdit(val draft: Recipe, val parsedIndex: Int? = null) : Screen
    data object Import : Screen
    data class PickRecipe(val day: Int, val meal: MealType) : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = Storage(app)
    private val saves = Channel<AppData>(Channel.CONFLATED)

    var data by mutableStateOf(storage.load())
        private set

    var tab by mutableStateOf(Tab.RECIPES)
    val stack = mutableStateListOf<Screen>()

    var importText by mutableStateOf("")
    var importBusy by mutableStateOf(false)
        private set
    var importMessage by mutableStateOf<String?>(null)
    val parsed = mutableStateListOf<Recipe>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for (snapshot in saves) storage.save(snapshot)
        }
    }

    private fun update(change: (AppData) -> AppData) {
        data = change(data)
        saves.trySend(data)
    }

    fun push(screen: Screen) {
        stack += screen
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun recipe(id: Long) = data.recipes.firstOrNull { it.id == id }

    fun saveRecipe(recipe: Recipe, parsedIndex: Int?) {
        update { d ->
            if (recipe.id == 0L) {
                d.copy(recipes = d.recipes + recipe.copy(id = d.nextId), nextId = d.nextId + 1)
            } else {
                d.copy(recipes = d.recipes.map { if (it.id == recipe.id) recipe else it })
            }
        }
        if (parsedIndex != null && parsedIndex in parsed.indices) parsed.removeAt(parsedIndex)
    }

    fun saveAllParsed() {
        parsed.toList().forEach { saveRecipe(it, null) }
        val n = parsed.size
        parsed.clear()
        importMessage = "Zapisano przepisy: $n"
    }

    fun deleteRecipe(id: Long) = update { d ->
        d.copy(
            recipes = d.recipes.filterNot { it.id == id },
            plan = d.plan.filterValues { it != id },
        )
    }

    fun toggleFavorite(id: Long) = update { d ->
        d.copy(recipes = d.recipes.map { if (it.id == id) it.copy(favorite = !it.favorite) else it })
    }

    fun setPlan(day: Int, meal: MealType, recipeId: Long?) = update { d ->
        val key = planKey(day, meal)
        d.copy(plan = if (recipeId == null) d.plan - key else d.plan + (key to recipeId))
    }

    fun toggleBought(key: String) = update { d ->
        d.copy(bought = if (key in d.bought) d.bought - key else d.bought + key)
    }

    fun setIngredientInfo(key: String, stock: Double?, packageSize: Double?) = update { d ->
        d.copy(
            pantry = if (stock == null || stock <= 0) d.pantry - key else d.pantry + (key to stock),
            packageSizes = if (packageSize == null || packageSize <= 0) d.packageSizes - key
            else d.packageSizes + (key to packageSize),
        )
    }

    fun closeWeek() = update { Shopping.closeWeek(it) }

    fun runOcr(uri: Uri, isPdf: Boolean = false) {
        if (importBusy) return
        importBusy = true
        importMessage = null
        viewModelScope.launch {
            try {
                val text = if (isPdf) Ocr.recognizePdf(getApplication(), uri)
                else Ocr.recognize(getApplication(), uri)
                importText = listOf(importText.trim(), text.trim()).filter { it.isNotEmpty() }.joinToString("\n\n")
                if (text.isBlank()) importMessage = "Nie rozpoznano tekstu. Spróbuj zrobić zdjęcie bliżej i przy lepszym świetle."
                else parseImport()
            } catch (e: Exception) {
                importMessage = "Błąd rozpoznawania: ${e.message}"
            } finally {
                importBusy = false
            }
        }
    }

    fun parseImport() {
        parsed.clear()
        parsed.addAll(RecipeParser.parse(importText))
        importMessage = if (parsed.isEmpty()) {
            "Nie znaleziono przepisów. Składniki powinny mieć postać „Nazwa - 100 g (1 x Sztuka)”. Możesz poprawić tekst i spróbować ponownie."
        } else {
            "Znaleziono przepisy: ${parsed.size}. Sprawdź je przed zapisaniem."
        }
    }
}
