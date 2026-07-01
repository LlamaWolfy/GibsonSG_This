package com.gibsonsg.todo.core.domain.model

import java.time.LocalDate

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * v1 recurrence is intentionally simple: "spawn the next instance when this one is
 * completed," not full RFC5545 expansion. [interval] is the number of [frequency]
 * units between occurrences (e.g. frequency=WEEKLY, interval=2 -> every 2 weeks).
 */
data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1
) {
    fun nextDueDate(from: LocalDate): LocalDate = when (frequency) {
        RecurrenceFrequency.DAILY -> from.plusDays(interval.toLong())
        RecurrenceFrequency.WEEKLY -> from.plusWeeks(interval.toLong())
        RecurrenceFrequency.MONTHLY -> from.plusMonths(interval.toLong())
        RecurrenceFrequency.YEARLY -> from.plusYears(interval.toLong())
    }

    fun serialize(): String = "FREQ=${frequency.name};INTERVAL=$interval"

    companion object {
        fun parse(raw: String?): RecurrenceRule? {
            if (raw.isNullOrBlank()) return null
            val parts = raw.split(";").associate {
                val (key, value) = it.split("=", limit = 2)
                key to value
            }
            val freq = parts["FREQ"]?.let { runCatching { RecurrenceFrequency.valueOf(it) }.getOrNull() }
                ?: return null
            val interval = parts["INTERVAL"]?.toIntOrNull() ?: 1
            return RecurrenceRule(freq, interval)
        }
    }
}
