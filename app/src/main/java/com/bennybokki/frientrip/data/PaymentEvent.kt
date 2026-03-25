package com.bennybokki.frientrip.data

data class PaymentEvent(
    val id: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val actorName: String = "",
    val timestamp: Long = 0L
)
