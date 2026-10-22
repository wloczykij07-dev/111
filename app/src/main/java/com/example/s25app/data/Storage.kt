package com.example.s25app.data

import android.content.Context
import com.example.s25app.model.AppData
import kotlinx.serialization.json.Json
import java.io.File

/** Wszystkie dane trzymane są w jednym pliku JSON w pamięci aplikacji. */
class Storage(context: Context) {
    private val file = File(context.filesDir, "data.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun load(): AppData {
        if (!file.exists()) {
            return AppData(recipes = SeedRecipes.all, nextId = SeedRecipes.all.maxOf { it.id } + 1)
        }
        return runCatching { json.decodeFromString<AppData>(file.readText()) }
            .getOrElse {
                // Nie nadpisuj uszkodzonego pliku bez kopii.
                file.copyTo(File(file.parentFile, "data.broken.json"), overwrite = true)
                AppData()
            }
    }

    @Synchronized
    fun save(data: AppData) {
        val tmp = File(file.parentFile, "data.json.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), data))
        tmp.renameTo(file)
    }
}
