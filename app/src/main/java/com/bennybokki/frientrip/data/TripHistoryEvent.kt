package com.bennybokki.frientrip.data

data class TripHistoryEvent(
    val id: String = "",
    val category: String = "",       // "expenses" | "supplies" | "payments"
    val description: String = "",
    val timestamp: Long = 0L
)
