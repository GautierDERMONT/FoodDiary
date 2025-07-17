package com.fooddiary.utils

import java.text.SimpleDateFormat
import java.util.*

fun getCurrentDayShort(): String {
    val calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
    }
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Lun"
        Calendar.TUESDAY -> "Mar"
        Calendar.WEDNESDAY -> "Mer"
        Calendar.THURSDAY -> "Jeu"
        Calendar.FRIDAY -> "Ven"
        Calendar.SATURDAY -> "Sam"
        Calendar.SUNDAY -> "Dim"
        else -> "Lun"
    }
}

fun getFormattedDate(dayShort: String): String {
    val calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY


        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)


        when (dayShort) {
            "Lun" -> {} // déjà à lundi
            "Mar" -> add(Calendar.DAY_OF_YEAR, 1)
            "Mer" -> add(Calendar.DAY_OF_YEAR, 2)
            "Jeu" -> add(Calendar.DAY_OF_YEAR, 3)
            "Ven" -> add(Calendar.DAY_OF_YEAR, 4)
            "Sam" -> add(Calendar.DAY_OF_YEAR, 5)
            "Dim" -> add(Calendar.DAY_OF_YEAR, 6)
        }
    }

    return SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH).format(calendar.time)
}