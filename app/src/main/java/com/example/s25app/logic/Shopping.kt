package com.example.s25app.logic

import com.example.s25app.model.AppData
import com.example.s25app.model.Recipe
import com.example.s25app.model.ingredientKey
import kotlin.math.ceil
import kotlin.math.max

data class ShoppingItem(
    val key: String,
    val name: String,
    val unit: String,
    /** ile potrzeba na cały plan */
    val needed: Double,
    /** ile jest w domu */
    val inStock: Double,
    /** ile brakuje (needed - inStock, nie mniej niż 0) */
    val missing: Double,
    val packageSize: Double?,
    val packages: Int,
    /** ile faktycznie kupić (z zaokrągleniem do opakowań) */
    val toBuy: Double,
    /** ile zostanie po tygodniu z tego, co kupisz */
    val leftover: Double,
    /** suma miar domowych, np. 3 x Sztuka, gdy wszystkie przepisy używają tej samej */
    val householdQty: Double?,
    val householdUnit: String,
    val usedIn: List<String>,
)

object Shopping {

    fun plannedRecipes(data: AppData): List<Recipe> {
        val byId = data.recipes.associateBy { it.id }
        return data.plan.values.mapNotNull { byId[it] }
    }

    fun compute(data: AppData): List<ShoppingItem> {
        class Acc(val name: String, val unit: String) {
            var needed = 0.0
            var householdQty = 0.0
            val householdUnits = mutableSetOf<String>()
            val usedIn = linkedSetOf<String>()
        }

        val acc = linkedMapOf<String, Acc>()
        for (recipe in plannedRecipes(data)) {
            for (ing in recipe.ingredients) {
                val key = ingredientKey(ing.name, ing.unit)
                val a = acc.getOrPut(key) { Acc(ing.name.trim(), ing.unit) }
                a.needed += ing.amount
                a.householdQty += ing.householdQty ?: 0.0
                a.householdUnits += ing.householdUnit.trim().lowercase()
                a.usedIn += recipe.name
            }
        }

        return acc.map { (key, a) ->
            val stock = data.pantry[key] ?: 0.0
            val missing = max(0.0, a.needed - stock)
            val size = data.packageSizes[key]?.takeIf { it > 0 }
            val packages = if (size != null && missing > 0) ceil(missing / size - 1e-9).toInt() else 0
            val toBuy = if (size != null) packages * size else missing
            val singleUnit = a.householdUnits.singleOrNull()?.takeIf { it.isNotEmpty() }
            ShoppingItem(
                key = key,
                name = a.name,
                unit = a.unit,
                needed = a.needed,
                inStock = stock,
                missing = missing,
                packageSize = size,
                packages = packages,
                toBuy = toBuy,
                leftover = max(0.0, stock + toBuy - a.needed),
                householdQty = if (singleUnit != null && a.householdQty > 0) a.householdQty else null,
                householdUnit = singleUnit ?: "",
                usedIn = a.usedIn.toList(),
            )
        }.sortedBy { it.name.lowercase() }
    }

    /** Przepisy spoza planu, które zużyją resztkę danego składnika. */
    fun recipesUsing(data: AppData, key: String): List<Recipe> {
        val planned = data.plan.values.toSet()
        return data.recipes.filter { r ->
            r.id !in planned && r.ingredients.any { ingredientKey(it.name, it.unit) == key }
        }
    }

    /**
     * Zamyka tydzień: to, co odhaczone jako kupione, trafia do zapasów,
     * zużyte w planie składniki są odejmowane, a plan i lista się czyszczą.
     */
    fun closeWeek(data: AppData): AppData {
        val pantry = data.pantry.toMutableMap()
        for (item in compute(data)) {
            val bought = if (item.key in data.bought) item.toBuy else 0.0
            val remaining = item.inStock + bought - item.needed
            if (remaining > 0.0) pantry[item.key] = remaining else pantry.remove(item.key)
        }
        return data.copy(plan = emptyMap(), pantry = pantry, bought = emptySet())
    }

    fun asText(items: List<ShoppingItem>): String = buildString {
        appendLine("Lista zakupów")
        for (item in items.filter { it.toBuy > 0 }) {
            append("• ").append(item.name).append(" – ").append(fmt(item.toBuy)).append(' ').append(item.unit)
            if (item.packageSize != null) {
                append(" (").append(item.packages).append(" × ").append(fmt(item.packageSize)).append(' ').append(item.unit).append(')')
            }
            appendLine()
        }
    }.trim()
}
