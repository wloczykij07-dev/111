package com.example.s25app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.s25app.logic.fmt
import com.example.s25app.logic.parseNumber
import com.example.s25app.model.Ingredient
import com.example.s25app.model.MealType
import com.example.s25app.model.Nutrition
import com.example.s25app.model.Recipe

@Composable
fun MealTypeChips(
    selected: MealType?,
    onSelect: (MealType?) -> Unit,
    allowAll: Boolean,
    favoritesOnly: Boolean? = null,
    onFavoritesChange: (Boolean) -> Unit = {},
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (favoritesOnly != null) {
            FilterChip(
                selected = favoritesOnly,
                onClick = { onFavoritesChange(!favoritesOnly) },
                label = { Text("Ulubione") },
                leadingIcon = {
                    Icon(
                        if (favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = FavoriteRed,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FavoriteRed.copy(alpha = 0.18f),
                ),
            )
        }
        if (allowAll) {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Wszystkie") })
        }
        MealType.entries.forEach { type ->
            val colors = mealColors(type)
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text("${type.emoji} ${type.label}") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colors.container.copy(alpha = 0.5f),
                    selectedContainerColor = colors.accent,
                    selectedLabelColor = Color.White,
                ),
                border = null,
            )
        }
    }
}

@Composable
fun RecipesScreen(vm: AppViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<MealType?>(null) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val list = vm.data.recipes
        .filter { filter == null || it.mealType == filter }
        .filter { !favoritesOnly || it.favorite }
        .filter { matchesQuery(it, query) }
        .sortedWith(compareBy({ !it.favorite }, { it.name.lowercase() }))

    ScreenScaffold(
        title = "Przepisy",
        inTab = true,
        actions = { TextButton(onClick = { vm.push(Screen.Import) }) { Text("Importuj") } },
        fab = {
            FloatingActionButton(onClick = {
                vm.push(Screen.RecipeEdit(Recipe(name = "", mealType = filter ?: MealType.BREAKFAST)))
            }) { Icon(Icons.Default.Add, contentDescription = "Dodaj przepis") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            SearchField(query, { query = it }, "Szukaj po nazwie lub składniku")
            Spacer(Modifier.height(8.dp))
            MealTypeChips(filter, { filter = it }, allowAll = true, favoritesOnly, { favoritesOnly = it })
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            ) {
                if (list.isEmpty()) {
                    item {
                        Text(
                            when {
                                vm.data.recipes.isEmpty() -> "Brak przepisów. Dodaj pierwszy przyciskiem + albo zaimportuj zdjęcie."
                                favoritesOnly && vm.data.recipes.none { it.favorite } ->
                                    "Nie masz jeszcze ulubionych. Dotknij serca przy przepisie, aby go dodać."
                                else -> "Nic nie pasuje do wyszukiwania."
                            },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(list, key = { it.id }) { r ->
                    RecipeCard(r, query, onToggleFavorite = { vm.toggleFavorite(r.id) }) {
                        vm.push(Screen.RecipeDetail(r.id))
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeDetailScreen(vm: AppViewModel, id: Long) {
    val recipe = vm.recipe(id)
    var confirmDelete by remember { mutableStateOf(false) }
    if (recipe == null) {
        LaunchedEffect(id) { vm.pop() }
        return
    }
    ScreenScaffold(
        title = recipe.name,
        onBack = vm::pop,
        actions = {
            IconButton(onClick = { vm.toggleFavorite(recipe.id) }) {
                Icon(
                    if (recipe.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (recipe.favorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                    tint = FavoriteRed,
                )
            }
            IconButton(onClick = { vm.push(Screen.RecipeEdit(recipe)) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edytuj")
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń")
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "${recipe.mealType.emoji} ${recipe.mealType.label}",
                color = mealColors(recipe.mealType).accent,
                style = MaterialTheme.typography.titleMedium,
            )
            NutritionTable(recipe.nutrition)
            SectionTitle("Składniki")
            recipe.ingredients.forEach { i ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(i.name, Modifier.weight(1f))
                    Text(ingredientAmount(i), style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
            }
            if (recipe.steps.isNotBlank()) {
                SectionTitle("Przygotowanie")
                Text(recipe.steps)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Usunąć przepis?") },
            text = { Text("„${recipe.name}” zniknie też z planu tygodnia.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.pop()
                    vm.deleteRecipe(recipe.id)
                }) { Text("Usuń") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Anuluj") } },
        )
    }
}

@Composable
fun NutritionTable(n: Nutrition) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "kcal" to n.kcal, "białko" to n.protein, "tłuszcz" to n.fat,
            "węgl." to n.carbs, "błonnik" to n.fiber, "WW" to n.ww,
        ).forEachIndexed { index, (label, value) ->
            val color = NutrientColors[index]
            Column(
                Modifier.weight(1f)
                    .background(color.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(fmt(value), style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private class IngredientDraft(
    name: String = "",
    amount: String = "",
    unit: String = "g",
    householdQty: String = "",
    householdUnit: String = "",
) {
    var name by mutableStateOf(name)
    var amount by mutableStateOf(amount)
    var unit by mutableStateOf(unit)
    var householdQty by mutableStateOf(householdQty)
    var householdUnit by mutableStateOf(householdUnit)
}

@Composable
fun RecipeEditScreen(vm: AppViewModel, screen: Screen.RecipeEdit) {
    val draft = screen.draft
    var name by remember(screen) { mutableStateOf(draft.name) }
    var mealType by remember(screen) { mutableStateOf(draft.mealType) }
    var steps by remember(screen) { mutableStateOf(draft.steps) }
    val n = draft.nutrition
    fun numText(v: Double) = if (v == 0.0) "" else fmt(v)
    var kcal by remember(screen) { mutableStateOf(numText(n.kcal)) }
    var protein by remember(screen) { mutableStateOf(numText(n.protein)) }
    var fat by remember(screen) { mutableStateOf(numText(n.fat)) }
    var carbs by remember(screen) { mutableStateOf(numText(n.carbs)) }
    var fiber by remember(screen) { mutableStateOf(numText(n.fiber)) }
    var ww by remember(screen) { mutableStateOf(numText(n.ww)) }
    val ingredients = remember(screen) {
        mutableStateListOf<IngredientDraft>().apply {
            draft.ingredients.forEach {
                add(IngredientDraft(it.name, fmt(it.amount), it.unit, it.householdQty?.let(::fmt) ?: "", it.householdUnit))
            }
            if (isEmpty()) add(IngredientDraft())
        }
    }
    var error by remember(screen) { mutableStateOf<String?>(null) }

    fun save() {
        if (name.isBlank()) {
            error = "Podaj nazwę przepisu."
            return
        }
        val parsedIngredients = ingredients.filter { it.name.isNotBlank() }.map {
            val amount = parseNumber(it.amount)
            if (amount == null) {
                error = "Podaj ilość dla: ${it.name}"
                return
            }
            Ingredient(
                name = it.name.trim(),
                amount = amount,
                unit = it.unit,
                householdQty = parseNumber(it.householdQty),
                householdUnit = it.householdUnit.trim(),
            )
        }
        fun num(s: String) = parseNumber(s) ?: 0.0
        vm.saveRecipe(
            draft.copy(
                name = name.trim(),
                mealType = mealType,
                ingredients = parsedIngredients,
                nutrition = Nutrition(num(kcal), num(protein), num(fat), num(carbs), num(fiber), num(ww)),
                steps = steps.trim(),
            ),
            screen.parsedIndex,
        )
        vm.pop()
    }

    ScreenScaffold(
        title = if (draft.id == 0L) "Nowy przepis" else "Edycja przepisu",
        onBack = vm::pop,
        actions = { TextButton(onClick = ::save) { Text("Zapisz") } },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(name, { name = it }, label = { Text("Nazwa") }, modifier = Modifier.fillMaxWidth())
            MealTypeChips(mealType, { if (it != null) mealType = it }, allowAll = false)

            SectionTitle("Wartości odżywcze (cały przepis)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(kcal, { kcal = it }, "kcal", Modifier.weight(1f))
                NumberField(protein, { protein = it }, "Białko", Modifier.weight(1f))
                NumberField(fat, { fat = it }, "Tłuszcz", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(carbs, { carbs = it }, "Węglow.", Modifier.weight(1f))
                NumberField(fiber, { fiber = it }, "Błonnik", Modifier.weight(1f))
                NumberField(ww, { ww = it }, "WW", Modifier.weight(1f))
            }

            SectionTitle("Składniki")
            ingredients.forEachIndexed { index, ing ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                ing.name, { ing.name = it },
                                label = { Text("Składnik ${index + 1}") },
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { ingredients.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Usuń składnik")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberField(ing.amount, { ing.amount = it }, "Ilość", Modifier.weight(1f))
                            OutlinedButton(onClick = { ing.unit = if (ing.unit == "g") "ml" else "g" }) { Text(ing.unit) }
                            NumberField(ing.householdQty, { ing.householdQty = it }, "Miara", Modifier.weight(0.8f))
                            OutlinedTextField(
                                ing.householdUnit, { ing.householdUnit = it },
                                label = { Text("np. Łyżka", maxLines = 1) },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f),
                            )
                        }
                    }
                }
            }
            OutlinedButton(onClick = { ingredients.add(IngredientDraft()) }) { Text("+ Dodaj składnik") }

            SectionTitle("Przygotowanie")
            OutlinedTextField(steps, { steps = it }, modifier = Modifier.fillMaxWidth(), minLines = 4)
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth()) { Text("Zapisz przepis") }
        }
    }
}
