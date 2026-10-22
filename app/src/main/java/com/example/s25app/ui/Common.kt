package com.example.s25app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.s25app.logic.fmt
import com.example.s25app.logic.parseNumber
import com.example.s25app.model.Ingredient
import com.example.s25app.model.Nutrition
import com.example.s25app.model.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    inTab: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                        }
                    }
                },
                actions = actions,
            )
        },
        floatingActionButton = fab,
        // Na zakładkach dolne wcięcie obsługuje pasek nawigacji aplikacji.
        contentWindowInsets = if (inTab) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets,
        content = content,
    )
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                }
            }
        },
    )
}

/** Usuwa polskie znaki, żeby "ogorek" znajdowało "Ogórek". */
fun normalizeForSearch(text: String): String {
    val from = "ąćęłńóśźż"
    val to = "acelnoszz"
    return text.lowercase().map { c -> from.indexOf(c).let { if (it >= 0) to[it] else c } }.joinToString("")
}

/** Wszystkie słowa zapytania muszą wystąpić w nazwie lub składnikach. */
fun matchesQuery(recipe: Recipe, query: String): Boolean {
    val terms = normalizeForSearch(query).split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
    if (terms.isEmpty()) return true
    val haystack = normalizeForSearch(recipe.name + " " + recipe.ingredients.joinToString(" ") { it.name })
    return terms.all { haystack.contains(it) }
}

fun matchedIngredients(recipe: Recipe, query: String): List<Ingredient> {
    val terms = normalizeForSearch(query).split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
    if (terms.isEmpty()) return emptyList()
    return recipe.ingredients.filter { i -> terms.any { normalizeForSearch(i.name).contains(it) } }
}

fun nutritionShort(n: Nutrition) =
    "${fmt(n.kcal)} kcal · B ${fmt(n.protein)} · T ${fmt(n.fat)} · W ${fmt(n.carbs)}"

fun ingredientAmount(i: Ingredient): String {
    val base = "${fmt(i.amount)} ${i.unit}"
    val qty = i.householdQty
    return if (qty != null && qty > 0 && i.householdUnit.isNotBlank()) "$base (${fmt(qty)} × ${i.householdUnit})" else base
}

@Composable
fun RecipeCard(recipe: Recipe, query: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(recipe.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${recipe.mealType.label} · ${nutritionShort(recipe.nutrition)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val matched = matchedIngredients(recipe, query)
            if (matched.isNotEmpty()) {
                Text(
                    "Zawiera: " + matched.joinToString { "${it.name} ${fmt(it.amount)} ${it.unit}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        isError = value.isNotBlank() && parseNumber(value) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

/** Okno do wpisania, ile składnika jest w domu i w jakich opakowaniach się go kupuje. */
@Composable
fun IngredientInfoDialog(
    name: String,
    unit: String,
    stock: Double?,
    packageSize: Double?,
    onDismiss: () -> Unit,
    onSave: (stock: Double?, packageSize: Double?) -> Unit,
) {
    var stockText by remember { mutableStateOf(stock?.let { fmt(it) } ?: "") }
    var packageText by remember { mutableStateOf(packageSize?.let { fmt(it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(stockText, { stockText = it }, "Mam w domu ($unit)", Modifier.fillMaxWidth())
                NumberField(packageText, { packageText = it }, "Opakowanie w sklepie ($unit)", Modifier.fillMaxWidth())
                Text(
                    "Wielkość opakowania pozwala policzyć, ile naprawdę kupisz i ile zostanie. Zostaw puste, jeśli kupujesz na wagę.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(parseNumber(stockText), parseNumber(packageText)) }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}
