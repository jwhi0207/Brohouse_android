package com.thiccbokki.brohouse.data

data class HouseDetails(
    val houseURL: String = "",
    val totalNights: Int = 0,
    val totalCost: Double = 0.0,
    val thumbnailURL: String? = null,
    val checkInMillis: Long = 0L,
    val checkOutMillis: Long = 0L
)
