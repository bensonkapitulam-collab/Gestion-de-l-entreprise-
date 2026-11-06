package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val frenchNumberFormat: NumberFormat by lazy {
    NumberFormat.getNumberInstance(Locale.FRANCE).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
}

fun formatCurrency(amount: Double): String {
    return "${frenchNumberFormat.format(amount)} €"
}

fun todayDateKey(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

fun currentMonthKey(): String {
    return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
}

fun formatDisplayDate(dateKey: String): String {
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)
        if (parsed != null) {
            SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(parsed)
        } else {
            dateKey
        }
    } catch (_: Exception) {
        dateKey
    }
}

fun formatDisplayMonth(monthKey: String): String {
    return try {
        val parsed = SimpleDateFormat("yyyy-MM", Locale.US).parse(monthKey)
        if (parsed != null) {
            SimpleDateFormat("MMMM yyyy", Locale.FRANCE).format(parsed)
                .replaceFirstChar { it.uppercase(Locale.FRANCE) }
        } else {
            monthKey
        }
    } catch (_: Exception) {
        monthKey
    }
}

fun shiftDateKey(dateKey: String, deltaDays: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(dateKey) ?: Date()
        cal.add(Calendar.DAY_OF_MONTH, deltaDays)
        sdf.format(cal.time)
    } catch (_: Exception) {
        todayDateKey()
    }
}

fun shiftMonthKey(monthKey: String, deltaMonths: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(monthKey) ?: Date()
        cal.add(Calendar.MONTH, deltaMonths)
        sdf.format(cal.time)
    } catch (_: Exception) {
        currentMonthKey()
    }
}

fun last7DaysKeysAndLabels(): List<Pair<String, String>> {
    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFmt = SimpleDateFormat("EEE d", Locale.FRANCE)
    val cal = Calendar.getInstance()
    val result = mutableListOf<Pair<String, String>>()
    for (i in 6 downTo 0) {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_MONTH, -i)
        result.add(keyFmt.format(c.time) to labelFmt.format(c.time))
    }
    return result
}
