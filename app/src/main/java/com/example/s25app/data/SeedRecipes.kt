package com.example.s25app.data

import com.example.s25app.model.Ingredient
import com.example.s25app.model.MealType
import com.example.s25app.model.Nutrition
import com.example.s25app.model.Recipe

/** Przepisy przepisane z jadłospisu dietetyka, dodawane przy pierwszym uruchomieniu. */
object SeedRecipes {
    val all = listOf(
        Recipe(
            id = 1,
            name = "Ciabatta z kurczakiem",
            mealType = MealType.BREAKFAST,
            ingredients = listOf(
                Ingredient("Mięso z piersi kurczaka, bez skóry", 100.0, "g", 1.0, "Kawałek"),
                Ingredient("Ciabatta", 80.0, "g", 1.0, "Sztuka"),
                Ingredient("Sałata lodowa", 80.0, "g", 4.0, "Liść"),
                Ingredient("Ogórek", 60.0, "g", 1.5, "Sztuka"),
                Ingredient("Pietruszka, liście", 12.0, "g", 2.0, "Łyżeczka"),
                Ingredient("Oliwa z oliwek", 5.0, "g", 0.5, "Łyżka"),
                Ingredient("Curry", 2.0, "g", 2.0, "Szczypta"),
                Ingredient("Pieprz czarny mielony", 1.0, "g", 1.0, "Szczypta"),
            ),
            nutrition = Nutrition(kcal = 415.8, protein = 30.5, fat = 9.7, carbs = 49.7, fiber = 4.8, ww = 5.1),
            steps = """
                1. Mięso zamarynuj w oliwie z curry i pieprzem, a następnie pokrój na kawałki.
                2. Usmaż kurczaka na patelni grillowej.
                3. Bułkę przekrój na pół i połóż na niej sałatę, pietruszkę i pokrojonego ogórka.
                4. Gotowe mięso przełóż do bułki.
            """.trimIndent(),
        ),
        Recipe(
            id = 2,
            name = "Torcik mocy",
            mealType = MealType.SNACK,
            ingredients = listOf(
                Ingredient("Mieszanka owocowa, mrożona", 100.0, "g", 1.0, "Szklanka"),
                Ingredient("Borówki amerykańskie", 75.0, "g", 1.5, "Garść"),
                Ingredient("Jaja kurze całe", 56.0, "g", 1.0, "Sztuka"),
                Ingredient("Maślanka (1.5% tł.)", 50.0, "g", 0.2, "Szklanka"),
                Ingredient("Ser twarogowy chudy", 50.0, "g", 0.2, "Opakowanie"),
                Ingredient("Mąka pszenna (typ 2000, pełnoziarnista)", 40.0, "g", 2.7, "Łyżka"),
                Ingredient("Jogurt naturalny (0% tł.)", 20.0, "g", 1.0, "Łyżka"),
                Ingredient("Odżywka białkowa (WPC)", 20.0, "g", 0.6, "Miarka"),
                Ingredient("Otręby orkiszowe", 4.0, "g", 1.0, "Łyżka"),
                Ingredient("Proszek do pieczenia", 1.0, "g", 0.3, "Łyżeczka"),
                Ingredient("Banan", 0.5, "g", 0.0, "Sztuka"),
            ),
            nutrition = Nutrition(kcal = 476.4, protein = 35.2, fat = 11.5, carbs = 49.4, fiber = 11.0, ww = 5.0),
            steps = """
                1. Rozgnieć banana widelcem i dodaj żółtko, a następnie maślankę — wymieszaj.
                2. Przesiej mąkę i proszek do pieczenia przez sitko. Dodaj do masy i wymieszaj.
                *Otręby możesz zblendować jeśli masz blender (opcjonalnie)
                3. Białko ubij i delikatnie wmieszaj w masę, a następnie dodaj borówki.
                4. Smaż na delikatnie rozgrzanej patelni teflonowej z łyżką wody, pod przykryciem.
                5. Gdy z wierzchu będzie ścięty przewróć go na drugą stronę i smaż bez przykrycia na złoty kolor.
                *jeśli nie jest teflonowa lub słabej jakości — delikatnie przesmaruj oliwą/olejem.
                *możesz również go upiec:
                6. Przełóż do formy i piecz w 160°C, 10 min z odrobiną wody w piekarniku.
                7. Jogurt (może być owocowy) wymieszaj z twarogiem.
                8. Pancakes ułóż w torcik i podawaj z twarogiem i ulubionymi owocami.
            """.trimIndent(),
        ),
    )
}
