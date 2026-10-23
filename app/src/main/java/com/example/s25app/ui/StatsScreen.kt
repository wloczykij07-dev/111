package com.example.s25app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.s25app.logic.Diary
import com.example.s25app.logic.fmt
import java.time.LocalDate

private val PERIODS = listOf("7 dni" to 7L, "30 dni" to 30L, "Cała historia" to null)

@Composable
fun StatsScreen(vm: AppViewModel) {
    var period by rememberSaveable { mutableIntStateOf(0) }
    val data = vm.data
    val days = PERIODS[period].second
    val fromDate = days?.let { LocalDate.now().minusDays(it - 1).toString() }
    val stats = Diary.stats(data.history, fromDate)
    val total = stats.sumOf { it.count }
    val plannedCount = data.plan.size
    val eatenCount = data.eaten.size

    ScreenScaffold(title = "Statystyki", inTab = true) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PERIODS.forEachIndexed { index, (label, _) ->
                        FilterChip(selected = period == index, onClick = { period = index }, label = { Text(label) })
                    }
                }
            }
            if (plannedCount > 0) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Bieżący plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Zjedzone $eatenCount z $plannedCount zaplanowanych posiłków")
                            LinearProgressIndicator(
                                progress = { eatenCount.toFloat() / plannedCount },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$total",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Column(Modifier.padding(start = 16.dp)) {
                            Text("zjedzonych posiłków", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "razem ${fmt(stats.sumOf { it.kcal })} kcal",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            if (total == 0) {
                item {
                    Text(
                        "Brak zjedzonych posiłków w tym okresie. Odhaczaj posiłki w zakładce Plan, a statystyki pojawią się tutaj.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(stats.filter { it.count > 0 }, key = { it.mealType.name }) { category ->
                val colors = mealColors(category.mealType)
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.container),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.mealType.emoji, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                category.mealType.label,
                                Modifier.weight(1f).padding(start = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                            )
                            Text(
                                "${category.count}×",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                            )
                        }
                        val max = category.recipes.maxOf { it.count }
                        category.recipes.forEach { recipe ->
                            Column {
                                Row {
                                    Text(recipe.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Text("${recipe.count}×", fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    Modifier.padding(top = 2.dp)
                                        .fillMaxWidth(recipe.count.toFloat() / max)
                                        .height(6.dp)
                                        .background(colors.accent, MaterialTheme.shapes.small),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
