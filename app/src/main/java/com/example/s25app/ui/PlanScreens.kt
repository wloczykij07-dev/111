package com.example.s25app.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.s25app.logic.Shopping
import com.example.s25app.logic.ShoppingItem
import com.example.s25app.logic.fmt
import com.example.s25app.model.MealType
import com.example.s25app.model.Nutrition
import com.example.s25app.model.ingredientKey
import com.example.s25app.model.planKey

val DAYS = listOf("Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota", "Niedziela")

@Composable
fun PlanScreen(vm: AppViewModel) {
    var confirmNewWeek by remember { mutableStateOf(false) }
    val data = vm.data
    ScreenScaffold(
        title = "Plan tygodnia",
        inTab = true,
        actions = {
            if (data.plan.isNotEmpty()) TextButton(onClick = { confirmNewWeek = true }) { Text("Nowy tydzień") }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(DAYS.indices.toList()) { day ->
                val recipes = MealType.entries.associateWith { meal ->
                    data.plan[planKey(day, meal)]?.let { vm.recipe(it) }
                }
                val total = recipes.values.filterNotNull().fold(Nutrition()) { acc, r -> acc + r.nutrition }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(DAYS[day], style = MaterialTheme.typography.titleMedium)
                        if (total.kcal > 0) {
                            Text(
                                nutritionShort(total),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        MealType.entries.forEach { meal ->
                            val r = recipes[meal]
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { vm.push(Screen.PickRecipe(day, meal)) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    meal.label,
                                    Modifier.width(96.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    r?.name ?: "wybierz…",
                                    Modifier.weight(1f),
                                    color = if (r == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                )
                                if (r != null) Text("${fmt(r.nutrition.kcal)} kcal", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmNewWeek) {
        AlertDialog(
            onDismissRequest = { confirmNewWeek = false },
            title = { Text("Zacząć nowy tydzień?") },
            text = {
                Text(
                    "Plan zostanie wyczyszczony. Składniki odhaczone na liście zakupów jako kupione, " +
                        "pomniejszone o zużyte w przepisach ilości, trafi do zapasów. " +
                        "Dzięki temu w następnym tygodniu nie kupisz tego, co już masz.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmNewWeek = false
                    vm.closeWeek()
                }) { Text("Tak, nowy tydzień") }
            },
            dismissButton = { TextButton(onClick = { confirmNewWeek = false }) { Text("Anuluj") } },
        )
    }
}

@Composable
fun PickRecipeScreen(vm: AppViewModel, screen: Screen.PickRecipe) {
    var query by rememberSaveable { mutableStateOf("") }
    var onlyThisMeal by rememberSaveable { mutableStateOf(true) }
    val current = vm.data.plan[planKey(screen.day, screen.meal)]
    val list = vm.data.recipes
        .filter { !onlyThisMeal || it.mealType == screen.meal }
        .filter { matchesQuery(it, query) }
        .sortedBy { it.name.lowercase() }

    ScreenScaffold(title = "${screen.meal.label} – ${DAYS[screen.day]}", onBack = vm::pop) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchField(query, { query = it }, "Szukaj po nazwie lub składniku")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = onlyThisMeal,
                            onClick = { onlyThisMeal = !onlyThisMeal },
                            label = { Text("Tylko: ${screen.meal.label.lowercase()}") },
                        )
                        if (current != null) {
                            OutlinedButton(onClick = {
                                vm.setPlan(screen.day, screen.meal, null)
                                vm.pop()
                            }) { Text("Usuń z planu") }
                        }
                    }
                }
            }
            if (list.isEmpty()) item { Text("Brak pasujących przepisów.", Modifier.padding(8.dp)) }
            items(list, key = { it.id }) { r ->
                RecipeCard(r, query) {
                    vm.setPlan(screen.day, screen.meal, r.id)
                    vm.pop()
                }
            }
        }
    }
}

@Composable
fun ShoppingScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val data = vm.data
    val items = remember(data) { Shopping.compute(data) }
    val toBuy = items.filter { it.toBuy > 0 }
    val covered = items.filter { it.toBuy <= 0 }
    var editing by remember { mutableStateOf<ShoppingItem?>(null) }

    ScreenScaffold(
        title = "Lista zakupów",
        inTab = true,
        actions = {
            if (toBuy.isNotEmpty()) {
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, Shopping.asText(items))
                    }
                    context.startActivity(Intent.createChooser(send, "Udostępnij listę"))
                }) { Icon(Icons.Default.Share, contentDescription = "Udostępnij") }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (items.isEmpty()) {
                item { Text("Lista jest pusta. Wybierz przepisy w zakładce Plan, a składniki zsumują się tutaj.") }
            }
            if (toBuy.isNotEmpty() && data.packageSizes.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(
                            "Wskazówka: dotknij składnika i wpisz, ile masz w domu oraz w jakich opakowaniach go kupujesz. " +
                                "Aplikacja policzy, ile kupić i co zostanie.",
                            Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (toBuy.isNotEmpty()) item { SectionTitle("Do kupienia (${toBuy.size})") }
            items(toBuy, key = { it.key }) { item ->
                Column {
                    ShoppingRow(vm, item, bought = item.key in data.bought, onEdit = { editing = item })
                    HorizontalDivider()
                }
            }
            if (covered.isNotEmpty()) item { SectionTitle("Masz już w domu") }
            items(covered, key = { "c" + it.key }) { item ->
                Column(Modifier.fillMaxWidth().clickable { editing = item }) {
                    Text(item.name, Modifier.padding(top = 8.dp))
                    Text(
                        "potrzebne ${fmt(item.needed)} ${item.unit} · masz ${fmt(item.inStock)} ${item.unit}",
                        Modifier.padding(bottom = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider()
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    editing?.let { item ->
        IngredientInfoDialog(
            name = item.name,
            unit = item.unit,
            stock = data.pantry[item.key],
            packageSize = data.packageSizes[item.key],
            onDismiss = { editing = null },
            onSave = { stock, size ->
                vm.setIngredientInfo(item.key, stock, size)
                editing = null
            },
        )
    }
}

@Composable
private fun ShoppingRow(vm: AppViewModel, item: ShoppingItem, bought: Boolean, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = bought, onCheckedChange = { vm.toggleBought(item.key) })
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    Modifier.weight(1f),
                    textDecoration = if (bought) TextDecoration.LineThrough else null,
                )
                Text("${fmt(item.toBuy)} ${item.unit}", fontWeight = FontWeight.Bold)
            }
            val details = buildList {
                add("potrzebne ${fmt(item.needed)} ${item.unit}")
                item.householdQty?.let { add("≈ ${fmt(it)} × ${item.householdUnit}") }
                if (item.inStock > 0) add("w domu ${fmt(item.inStock)} ${item.unit}")
            }
            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.packageSize != null) {
                Text(
                    "${item.packages} × opakowanie ${fmt(item.packageSize)} ${item.unit}" +
                        if (item.leftover > 0) " → zostanie ${fmt(item.leftover)} ${item.unit}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (item.leftover > 0) {
                    val ideas = Shopping.recipesUsing(vm.data, item.key).take(3)
                    if (ideas.isNotEmpty()) {
                        Text(
                            "Resztę wykorzystasz w: " + ideas.joinToString { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PantryScreen(vm: AppViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<Pair<String, Pair<String, String>>?>(null) }
    val data = vm.data
    // Wszystkie składniki z przepisów + to, co jest w zapasach.
    val known = remember(data) {
        val map = linkedMapOf<String, Pair<String, String>>()
        data.recipes.flatMap { it.ingredients }.forEach { i ->
            map.getOrPut(ingredientKey(i.name, i.unit)) { i.name.trim() to i.unit }
        }
        (data.pantry.keys + data.packageSizes.keys).forEach { key ->
            map.getOrPut(key) { key.substringBefore('|') to key.substringAfter('|') }
        }
        map
    }
    val rows = known.entries
        .filter { normalizeForSearch(it.value.first).contains(normalizeForSearch(query.trim())) }
        .sortedWith(compareBy({ it.key !in data.pantry }, { it.value.first.lowercase() }))

    ScreenScaffold(title = "Zapasy i opakowania", inTab = true) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Column {
                    Text(
                        "Wpisz, ile czego masz w domu i jakiej wielkości opakowania kupujesz. Lista zakupów odejmie zapasy i zaokrągli do opakowań.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    SearchField(query, { query = it }, "Szukaj składnika")
                }
            }
            items(rows, key = { it.key }) { (key, info) ->
                val (name, unit) = info
                Column(Modifier.fillMaxWidth().clickable { editing = key to info }) {
                    Text(name, Modifier.padding(top = 8.dp))
                    val parts = buildList {
                        data.pantry[key]?.let { add("w domu: ${fmt(it)} $unit") }
                        data.packageSizes[key]?.let { add("opakowanie: ${fmt(it)} $unit") }
                    }
                    Text(
                        parts.joinToString(" · ").ifEmpty { "brak danych – dotknij, aby uzupełnić" },
                        modifier = Modifier.padding(bottom = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (parts.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    editing?.let { (key, info) ->
        IngredientInfoDialog(
            name = info.first,
            unit = info.second,
            stock = data.pantry[key],
            packageSize = data.packageSizes[key],
            onDismiss = { editing = null },
            onSave = { stock, size ->
                vm.setIngredientInfo(key, stock, size)
                editing = null
            },
        )
    }
}
