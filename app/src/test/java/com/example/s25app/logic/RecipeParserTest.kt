package com.example.s25app.logic

import com.example.s25app.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeParserTest {

    /** Kolejność jak w PDF-ie: nagłówek, wartości, nazwa, składniki, kroki. */
    private val sequential = """
        ŚNIADANIE K:415.8 / B:30.5 / T:9.7 / WP:49.7 / F:4.8 / WW:5.1
        CIABATTA Z KURCZAKIEM
        Mięso z piersi kurczaka, bez skóry - 100 g (1 x
        Kawałek)
        Ciabatta - 80 g (1 x Sztuka)
        Sałata lodowa - 80 g (4 x Liść)
        Ogórek - 60 g (1.5 x Sztuka)
        Pietruszka, liście - 12 g (2 x Łyżeczka)
        Oliwa z oliwek - 5 g (0.5 x Łyżka)
        Curry - 2 g (2 x Szczypta)
        Pieprz czarny mielony - 1 g (1 x Szczypta)
        1. Mięso zamarynuj w oliwie z curry i pieprzem, a następnie
        pokrój na kawałki.
        2. Usmaż kurczaka na patelni grillowej.
        PRZEKĄSKA K:476.4 / B:35.2 / T:11.5 / WP:49.4 / F:11.0 / WW:5.0
        TORCIK MOCY
        Mieszanka owocowa, mrożona - 100 g (1 x
        Szklanka)
        Maślanka (1.5% tł.) - 50 g (0.2 x Szklanka)
        Mąka pszenna (typ 2000, pełnoziarnista) - 40 g (2.7
        x Łyżka)
        1. Rozgnieć banana widelcem.
        *Otręby możesz zblendować
        2. Przesiej mąkę.
    """.trimIndent()

    /** Kolejność typowa dla zdjęcia: najpierw lewa kolumna, potem prawa. */
    private val columns = """
        ŚNIADANIE
        CIABATTA Z KURCZAKIEM
        Ciabatta - 80 g (1 x Sztuka)
        Curry - 2 g (2 x Szczypta)
        PRZEKĄSKA
        TORCIK MOCY
        Jaja kurze całe - 56 g (1 x Sztuka)
        Banan - 0.5 g (0 x Sztuka)
        BANAN, BORÓWKI
        K:415.8 / B:30.5 / T:9.7 / WP:49.7 / F:4.8 / WW:5.1
        1. Mięso zamarynuj w oliwie.
        2. Usmaż kurczaka.
        K:476.4 / B:35.2 / T:11.5 / WP:49.4 / F:11.0 / WW:5.0
        1. Rozgnieć banana widelcem i dodaj żółtko, a następnie
        maślankę — wymieszaj.
        2. Przesiej mąkę.
    """.trimIndent()

    @Test
    fun parsesSequentialText() {
        val recipes = RecipeParser.parse(sequential)
        assertEquals(2, recipes.size)

        val ciabatta = recipes[0]
        assertEquals("Ciabatta z kurczakiem", ciabatta.name)
        assertEquals(MealType.BREAKFAST, ciabatta.mealType)
        assertEquals(8, ciabatta.ingredients.size)
        assertEquals(415.8, ciabatta.nutrition.kcal, 0.001)
        assertEquals(49.7, ciabatta.nutrition.carbs, 0.001)
        assertEquals(5.1, ciabatta.nutrition.ww, 0.001)

        val meat = ciabatta.ingredients[0]
        assertEquals("Mięso z piersi kurczaka, bez skóry", meat.name)
        assertEquals(100.0, meat.amount, 0.001)
        assertEquals("g", meat.unit)
        assertEquals(1.0, meat.householdQty!!, 0.001)
        assertEquals("Kawałek", meat.householdUnit)
        assertEquals(1.5, ciabatta.ingredients[3].householdQty!!, 0.001)
        assertTrue(ciabatta.steps.startsWith("1. Mięso zamarynuj"))
        assertTrue(ciabatta.steps.contains("następnie pokrój na kawałki."))

        val torcik = recipes[1]
        assertEquals("Torcik mocy", torcik.name)
        assertEquals(MealType.SNACK, torcik.mealType)
        assertEquals(476.4, torcik.nutrition.kcal, 0.001)
        assertEquals("Maślanka (1.5% tł.)", torcik.ingredients[1].name)
        assertEquals("Mąka pszenna (typ 2000, pełnoziarnista)", torcik.ingredients[2].name)
        assertEquals(2.7, torcik.ingredients[2].householdQty!!, 0.001)
        assertEquals("Łyżka", torcik.ingredients[2].householdUnit)
        assertEquals(3, torcik.steps.lines().size)
    }

    @Test
    fun parsesTwoColumnPhotoOrder() {
        val recipes = RecipeParser.parse(columns)
        assertEquals(2, recipes.size)
        assertEquals("Ciabatta z kurczakiem", recipes[0].name)
        assertEquals(415.8, recipes[0].nutrition.kcal, 0.001)
        assertTrue(recipes[0].steps.contains("Usmaż kurczaka"))
        assertEquals("Torcik mocy", recipes[1].name)
        assertEquals(476.4, recipes[1].nutrition.kcal, 0.001)
        assertTrue(recipes[1].steps.contains("maślankę — wymieszaj."))
        assertEquals(2, recipes[1].ingredients.size)
    }

    @Test
    fun fixesTextReadFromBottomToTop() {
        val reversed = sequential.lines().reversed().joinToString("\n")
        assertEquals(RecipeParser.parse(sequential), RecipeParser.parse(reversed))
    }

    @Test
    fun convertsKilogramsAndCommas() {
        val recipes = RecipeParser.parse(
            """
            OBIAD
            Zupa dyniowa
            Dynia - 0,5 kg (1 x Kawałek)
            Bulion - 1 l
            """.trimIndent()
        )
        assertEquals(1, recipes.size)
        assertEquals("Zupa dyniowa", recipes[0].name)
        assertEquals(MealType.LUNCH, recipes[0].mealType)
        assertEquals(500.0, recipes[0].ingredients[0].amount, 0.001)
        assertEquals("ml", recipes[0].ingredients[1].unit)
        assertEquals(1000.0, recipes[0].ingredients[1].amount, 0.001)
    }
}
