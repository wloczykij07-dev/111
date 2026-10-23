package com.example.s25app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.atan2
import kotlin.math.max

/**
 * Rozpoznawanie tekstu ze zdjęcia w telefonie (Google ML Kit, bez internetu).
 * Kartka bywa sfotografowana bokiem, więc próbujemy kilku obrotów
 * i wybieramy ten, z którego wyszło najwięcej sensownego tekstu.
 */
object Ocr {
    private const val MAX_SIDE = 3000

    suspend fun recognize(context: Context, uri: Uri): String {
        val bitmap = withContext(Dispatchers.IO) { loadBitmap(context, uri) }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            var best = ""
            var bestScore = -1
            val tried = mutableSetOf<Int>()
            val rotations = ArrayDeque(listOf(0, 90, 270, 180))
            while (rotations.isNotEmpty()) {
                val rotation = rotations.removeFirst()
                if (!tried.add(rotation)) continue
                val result = recognizer.process(InputImage.fromBitmap(rotate(bitmap, rotation), 0)).await()
                val text = result.text
                // ML Kit czyta też tekst leżący bokiem, ale kolejność linii liczy
                // względem obrazu, więc wychodzi np. od dołu do góry. Wtedy
                // najpierw próbujemy obrotu, przy którym tekst stoi prosto.
                val angle = dominantTextAngle(result)
                if (angle != 0) rotations.addFirst((rotation + 360 - angle) % 360)
                val upright = angle == 0 && ingredientLines(text) > 0
                val score = score(text) + if (upright) 1000 else 0
                if (score > bestScore) {
                    best = text
                    bestScore = score
                }
                // Wyraźnie dobry wynik z prosto stojącym tekstem - koniec.
                if (upright && ingredientLines(text) >= 3) break
            }
            return best
        } finally {
            recognizer.close()
        }
    }

    /** Każdą stronę PDF-a renderuje jako obraz i rozpoznaje z niej tekst. */
    suspend fun recognizePdf(context: Context, uri: Uri, maxPages: Int = 40): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val pages = mutableListOf<String>()
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: error("Nie udało się otworzyć pliku PDF")
            pfd.use {
                PdfRenderer(it).use { renderer ->
                    for (i in 0 until minOf(renderer.pageCount, maxPages)) {
                        val bitmap = withContext(Dispatchers.IO) {
                            renderer.openPage(i).use { page ->
                                val scale = 3
                                Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                                    .also { bmp ->
                                        bmp.eraseColor(Color.WHITE)
                                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    }
                            }
                        }
                        pages += recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
                        bitmap.recycle()
                    }
                }
            }
            return pages.joinToString("\n")
        } finally {
            recognizer.close()
        }
    }

    /** Przeważający kierunek tekstu na obrazie: 0, 90, 180 lub 270 stopni (zgodnie z ruchem wskazówek). */
    private fun dominantTextAngle(result: Text): Int {
        val votes = IntArray(4)
        for (block in result.textBlocks) {
            for (line in block.lines) {
                val p = line.cornerPoints ?: continue
                if (p.size < 2) continue
                val degrees = Math.toDegrees(atan2((p[1].y - p[0].y).toDouble(), (p[1].x - p[0].x).toDouble()))
                val quarter = (((degrees + 405) % 360) / 90).toInt() % 4
                votes[quarter] += line.text.length
            }
        }
        if (votes.sum() == 0) return 0
        return votes.indices.maxBy { votes[it] } * 90
    }

    private fun ingredientLines(text: String) =
        text.lines().count { Regex("""\s[-–—]\s*\d+([.,]\d+)?\s*(g|ml)\b""").containsMatchIn(it) }

    private fun score(text: String): Int {
        val words = Regex("""\p{L}{3,}""").findAll(text).count()
        return ingredientLines(text) * 20 + words
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            // ImageDecoder sam uwzględnia orientację zapisaną w EXIF.
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val side = max(info.size.width, info.size.height)
                if (side > MAX_SIDE) {
                    val scale = MAX_SIDE.toFloat() / side
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt(),
                        (info.size.height * scale).toInt(),
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: error("Nie udało się wczytać zdjęcia")
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
