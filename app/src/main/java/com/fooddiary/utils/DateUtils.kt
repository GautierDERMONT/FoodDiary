package com.fooddiary.utils

import java.text.SimpleDateFormat
import java.util.*

fun getCurrentDayShort(): String {
    return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
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
    val calendar = Calendar.getInstance()
    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    val targetDayOfWeek = when (dayShort) {
        "Lun" -> Calendar.MONDAY
        "Mar" -> Calendar.TUESDAY
        "Mer" -> Calendar.WEDNESDAY
        "Jeu" -> Calendar.THURSDAY
        "Ven" -> Calendar.FRIDAY
        "Sam" -> Calendar.SATURDAY
        "Dim" -> Calendar.SUNDAY
        else -> currentDayOfWeek
    }


    var diff = targetDayOfWeek - currentDayOfWeek

    if (diff < 0) diff += 1


    calendar.firstDayOfWeek = Calendar.MONDAY
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

    calendar.add(Calendar.DAY_OF_YEAR, diff)

    return SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH).format(calendar.time)
}