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



fun getCurrentWeekNumber(): Int {
    val calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
    }
    return calendar.get(Calendar.WEEK_OF_YEAR)
}

fun getWeekRange(): String {
    val calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
    }

    // Début de semaine (lundi)
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val startDate = SimpleDateFormat("d MMMM", Locale.FRENCH).format(calendar.time)

    // Fin de semaine (dimanche)
    calendar.add(Calendar.DAY_OF_YEAR, 6)
    val endDate = SimpleDateFormat("d MMMM yyyy", Locale.FRENCH).format(calendar.time)

    return "$startDate au $endDate"
}

// Dans DateUtils.kt
fun getCurrentWeekInfo(calendar: Calendar): String {
    val weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
    return "Semaine $weekNumber (${getWeekRangeForCalendar(calendar)})"
}

fun getWeekRangeForCalendar(calendar: Calendar): String {
    val startCal = calendar.clone() as Calendar
    startCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

    val endCal = calendar.clone() as Calendar
    endCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

    val startDate = SimpleDateFormat("d MMM", Locale.FRENCH).format(startCal.time)
    val endDate = SimpleDateFormat("d MMM yyyy", Locale.FRENCH).format(endCal.time)

    return "$startDate - $endDate"
}


fun getFormattedDate(dayShort: String, weekOffset: Int = 0): String {
    val calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        add(Calendar.WEEK_OF_YEAR, weekOffset) // Ajout du décalage de semaine

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