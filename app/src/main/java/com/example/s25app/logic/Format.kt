package com.example.s25app.logic

import kotlin.math.abs
import kotlin.math.roundToLong

/** 120.0 -> "120", 0.5 -> "0,5", 12.345 -> "12,3" */
fun fmt(value: Double): String {
    val rounded = (value * 10).roundToLong() / 10.0
    return if (abs(rounded - rounded.roundToLong()) < 1e-9) {
        rounded.roundToLong().toString()
    } else {
        rounded.toString().replace('.', ',')
    }
}

fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
