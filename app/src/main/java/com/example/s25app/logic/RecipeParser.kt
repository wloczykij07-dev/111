package com.example.s25app.logic

import com.example.s25app.model.Ingredient
import com.example.s25app.model.MealType
import com.example.s25app.model.Nutrition
import com.example.s25app.model.Recipe

/**
 * Odczytuje przepisy z tekstu w formacie z jadłospisu dietetyka, np.:
 *
 *   ŚNIADANIE  K:415.8 / B:30.5 / T:9.7 / WP:49.7 / F:4.8 / WW:5.1
 *   CIABATTA Z KURCZAKIEM
 *   Ciabatta - 80 g (1 x Sztuka)
 *   1. Bułkę przekrój na pół...
 *
 * Tekst może pochodzić z PDF-a albo z rozpoznawania zdjęcia, więc parser
 * jest wyrozumiały: łączy zawinięte linie i pomija to, czego nie rozumie.
 */
object RecipeParser {

    private val ingredientRegex = Regex(
        """^(.+?)\s+[-–—]\s*(\d+(?:[.,]\d+)?)\s*(kg|g|ml|l)\b\.?\s*(?:\((.*?)\)?)?\s*$""",
        RegexOption.IGNORE_CASE,
    )
    private val ingredientStartRegex = Regex(
        """\s[-–—]\s*\d+(?:[.,]\d+)?\s*(kg|g|ml|l)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val householdRegex = Regex("""^\s*(\d+(?:[.,]\d+)?)\s*[xX×]\s*(.+?)\s*$""")
    private val stepRegex = Regex("""^(\d{1,2})\s*[.)]\s*(.+)$""")

    private fun nutrientRegex(key: String) =
        Regex("""(?<![A-ZĄĆĘŁŃÓŚŹŻ])$key\s*[:;]\s*(\d+(?:[.,]\d+)?)""", RegexOption.IGNORE_CASE)

    private val kcalRegex = nutrientRegex("K")
    private val proteinRegex = nutrientRegex("B")
    private val fatRegex = nutrientRegex("T")
    private val carbsRegex = nutrientRegex("WP")
    private val fiberRegex = nutrientRegex("F")
    private val wwRegex = nutrientRegex("WW")
    private val nutritionPartRegex = Regex(
        """(?<![A-ZĄĆĘŁŃÓŚŹŻ])(K|B|T|WP|F|WW)\s*[:;]\s*\d+(?:[.,]\d+)?\s*/?""",
        RegexOption.IGNORE_CASE,
    )

    private class Builder(var rawName: String, val mealType: MealType) {
        val ingredients = mutableListOf<Ingredient>()
        val steps = mutableListOf<String>()
        var nutrition: Nutrition? = null
        var nameLines = 1

        fun build() = Recipe(
            name = prettifyName(rawName),
            mealType = mealType,
            ingredients = ingredients.toList(),
            nutrition = nutrition ?: Nutrition(),
            steps = steps.joinToString("\n"),
        )
    }

    fun parse(text: String): List<Recipe> {
        var rawLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        // Rozpoznawanie zdjęcia czasem zwraca linie od dołu do góry -
        // widać to po krokach numerowanych malejąco (8, 7, 6...).
        if (looksReversed(rawLines)) rawLines = rawLines.reversed()
        val lines = joinWrappedLines(rawLines)
        // Na zdjęciu strony z dwiema kolumnami rozpoznany tekst często zawiera
        // najpierw wszystkie nazwy i składniki, a dopiero potem wartości
        // odżywcze i kroki. Wtedy przypisujemy je kolejno do przepisów.
        val firstNutrition = lines.indexOfFirst { parseNutrition(it) != null }
        val firstIngredient = lines.indexOfFirst { ingredientRegex.matches(it) }
        val columnMode = firstNutrition >= 0 && firstIngredient >= 0 && firstNutrition > firstIngredient

        val result = mutableListOf<Builder>()
        var category: MealType? = null
        var current: Builder? = null
        var stepTarget: Builder? = null
        var pendingNutrition: Nutrition? = null

        fun startRecipe(name: String): Builder {
            val b = Builder(name, category ?: MealType.BREAKFAST)
            if (!columnMode && pendingNutrition != null) {
                b.nutrition = pendingNutrition
                pendingNutrition = null
            }
            result += b
            current = b
            stepTarget = b
            return b
        }

        for (raw in lines) {
            var line = raw

            val nutrition = parseNutrition(line)
            if (nutrition != null) {
                line = line.replace(nutritionPartRegex, "").trim()
                if (columnMode) {
                    result.firstOrNull { it.nutrition == null }?.nutrition = nutrition
                } else {
                    pendingNutrition = nutrition
                }
                if (line.isEmpty()) continue
            }

            val mealType = mealTypeOf(line)
            if (mealType != null) {
                category = mealType
                current = null
                if (!columnMode) stepTarget = null
                continue
            }

            val ingredient = ingredientRegex.matchEntire(line)
            if (ingredient != null) {
                val b = current ?: startRecipe("Nowy przepis")
                b.ingredients += toIngredient(ingredient)
                continue
            }

            val step = stepRegex.matchEntire(line)
            if (step != null) {
                val number = step.groupValues[1].toInt()
                if (columnMode && number == 1) {
                    stepTarget = result.firstOrNull { it.steps.isEmpty() } ?: stepTarget
                }
                val b = stepTarget ?: current ?: startRecipe("Nowy przepis")
                b.steps += "$number. ${step.groupValues[2]}"
                continue
            }

            val target = stepTarget
            if (line.startsWith("*")) {
                target?.steps?.add(line)
                continue
            }

            val cur = current
            if (isUpperCaseTitle(line)) {
                if (cur != null && cur.ingredients.isEmpty() && cur.steps.isEmpty() && cur.nameLines < 3) {
                    // Nazwa przepisu zawinięta na kilka linii.
                    cur.rawName = cur.rawName + " " + line
                    cur.nameLines++
                } else {
                    startRecipe(line)
                }
                continue
            }

            when {
                target != null && target.steps.isNotEmpty() ->
                    target.steps[target.steps.lastIndex] = target.steps.last() + " " + line
                cur == null && line.count { it.isLetter() } >= 3 -> startRecipe(line)
            }
        }

        return result
            .filter { it.ingredients.isNotEmpty() }
            .map { it.build() }
    }

    private fun looksReversed(lines: List<String>): Boolean {
        val numbers = lines.mapNotNull { stepRegex.matchEntire(it)?.groupValues?.get(1)?.toInt() }
        val pairs = numbers.zipWithNext()
        val descending = pairs.count { (a, b) -> b == a - 1 }
        val ascending = pairs.count { (a, b) -> b == a + 1 }
        return descending > ascending
    }

    /** Skleja linie składnika rozbite na kilka linii, np. "... - 100 g (1 x" + "Kawałek)". */
    private fun joinWrappedLines(lines: List<String>): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            var line = lines[i]
            if (ingredientStartRegex.containsMatchIn(line)) {
                var extra = 0
                while (line.count { it == '(' } > line.count { it == ')' } &&
                    i + 1 < lines.size && extra < 2
                ) {
                    i++
                    extra++
                    line = "$line ${lines[i]}"
                }
            }
            out += line
            i++
        }
        return out
    }

    private fun toIngredient(m: MatchResult): Ingredient {
        var amount = parseNumber(m.groupValues[2]) ?: 0.0
        var unit = m.groupValues[3].lowercase()
        when (unit) {
            "kg" -> { amount *= 1000; unit = "g" }
            "l" -> { amount *= 1000; unit = "ml" }
        }
        val household = householdRegex.matchEntire(m.groupValues[4].trim().trimEnd(')'))
        return Ingredient(
            name = m.groupValues[1].trim().trimEnd('-', '–', '—').trim(),
            amount = amount,
            unit = unit,
            householdQty = household?.let { parseNumber(it.groupValues[1]) },
            householdUnit = household?.groupValues?.get(2)?.trim() ?: "",
        )
    }

    fun parseNutrition(line: String): Nutrition? {
        val kcal = kcalRegex.find(line) ?: return null
        val protein = proteinRegex.find(line) ?: return null
        fun value(r: Regex) = r.find(line)?.groupValues?.get(1)?.let { parseNumber(it) } ?: 0.0
        return Nutrition(
            kcal = parseNumber(kcal.groupValues[1]) ?: 0.0,
            protein = parseNumber(protein.groupValues[1]) ?: 0.0,
            fat = value(fatRegex),
            carbs = value(carbsRegex),
            fiber = value(fiberRegex),
            ww = value(wwRegex),
        )
    }

    fun mealTypeOf(line: String): MealType? {
        val l = line.uppercase().replace(Regex("[^A-ZĄĆĘŁŃÓŚŹŻ ]"), "").trim()
        return when (l) {
            "ŚNIADANIE", "I ŚNIADANIE", "SNIADANIE" -> MealType.BREAKFAST
            "PRZEKĄSKA", "PRZEKASKA", "II ŚNIADANIE", "DRUGIE ŚNIADANIE", "PODWIECZOREK" -> MealType.SNACK
            "OBIAD" -> MealType.LUNCH
            "KOLACJA" -> MealType.DINNER
            else -> null
        }
    }

    private fun isUpperCaseTitle(line: String): Boolean {
        val letters = line.filter { it.isLetter() }
        if (letters.length < 3) return false
        return letters.count { it.isUpperCase() } >= letters.length * 0.8
    }

    /** "CIABATTA Z KURCZAKIEM" -> "Ciabatta z kurczakiem" */
    fun prettifyName(name: String): String {
        val n = name.trim().replace(Regex("\\s+"), " ")
        val letters = n.filter { it.isLetter() }
        if (letters.isEmpty() || letters.any { it.isLowerCase() } && n.first().isUpperCase()) return n
        val lower = n.lowercase()
        return lower.replaceFirstChar { it.uppercase() }
    }
}
