package com.example.s25app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.s25app.logic.fmt
import java.io.File

@Composable
fun ImportScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) photoUri?.let { vm.runOcr(Uri.parse(it)) }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { vm.runOcr(it) }
    }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.runOcr(it, isPdf = true) }
    }

    fun openCamera() {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        photoUri = uri.toString()
        takePicture.launch(uri)
    }

    ScreenScaffold(title = "Importuj przepisy", onBack = vm::pop) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Zrób zdjęcie kartki od dietetyka, wybierz zdjęcie lub PDF albo wklej tekst skopiowany z PDF-a. " +
                    "Tekst rozpoznaje telefon, bez internetu. Zawsze sprawdź wynik przed zapisaniem.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = ::openCamera, enabled = !vm.importBusy, modifier = Modifier.weight(1f)) {
                    Text("Zdjęcie")
                }
                OutlinedButton(
                    onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !vm.importBusy,
                    modifier = Modifier.weight(1f),
                ) { Text("Galeria") }
                OutlinedButton(
                    onClick = { pickPdf.launch(arrayOf("application/pdf")) },
                    enabled = !vm.importBusy,
                    modifier = Modifier.weight(1f),
                ) { Text("PDF") }
            }
            if (vm.importBusy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Rozpoznaję tekst…")
            }

            OutlinedTextField(
                value = vm.importText,
                onValueChange = { vm.importText = it },
                label = { Text("Tekst przepisów (możesz go poprawić)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                maxLines = 16,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = vm::parseImport, enabled = vm.importText.isNotBlank() && !vm.importBusy) {
                    Text("Rozpoznaj przepisy")
                }
                TextButton(onClick = {
                    vm.importText = ""
                    vm.parsed.clear()
                    vm.importMessage = null
                }) { Text("Wyczyść") }
            }
            vm.importMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            vm.parsed.forEachIndexed { index, r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${r.mealType.label} · ${r.ingredients.size} składników · ${fmt(r.nutrition.kcal)} kcal" +
                                if (r.steps.isBlank()) " · brak kroków" else "",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            r.ingredients.joinToString { "${it.name} ${fmt(it.amount)} ${it.unit}" },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                        )
                        Row {
                            TextButton(onClick = { vm.push(Screen.RecipeEdit(r, parsedIndex = index)) }) {
                                Text("Sprawdź i zapisz")
                            }
                            TextButton(onClick = { vm.parsed.removeAt(index) }) { Text("Odrzuć") }
                        }
                    }
                }
            }
            if (vm.parsed.size > 1) {
                OutlinedButton(onClick = vm::saveAllParsed, modifier = Modifier.fillMaxWidth()) {
                    Text("Zapisz wszystkie bez sprawdzania (${vm.parsed.size})")
                }
            }
        }
    }
}
