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
    val calendar = Calendar.getInstance().apply {
        // On commence par définir le premier jour de la semaine (lundi)
        firstDayOfWeek = Calendar.MONDAY
        // On se positionne sur le lundi de la semaine en cours
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    }

    // On ajoute le nombre de jours correspondant au jour demandé
    when (dayShort) {
        "Lun" -> {} // déjà sur lundi
        "Mar" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        "Mer" -> calendar.add(Calendar.DAY_OF_YEAR, 2)
        "Jeu" -> calendar.add(Calendar.DAY_OF_YEAR, 3)
        "Ven" -> calendar.add(Calendar.DAY_OF_YEAR, 4)
        "Sam" -> calendar.add(Calendar.DAY_OF_YEAR, 5)
        "Dim" -> calendar.add(Calendar.DAY_OF_YEAR, 6)
    }

    return SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH).format(calendar.time)
}