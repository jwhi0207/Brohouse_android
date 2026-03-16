package com.bennybokki.frientrip.data

data class Ride(
    val id: String = "",
    val driverUid: String = "",
    val driverName: String = "",
    val vehicleEmoji: String = "🚗",
    val vehicleLabel: String = "",
    val departureLocation: String = "",
    val totalSeats: Int = 4,
    val departureTime: Long = 0L,
    val returnTime: Long = 0L,
    val notes: String = "",
    val passengerUids: List<String> = emptyList(),
    val passengerNames: List<String> = emptyList()
) {
    val availableSeats: Int get() = totalSeats - passengerUids.size
    val isFull: Boolean get() = availableSeats <= 0
}
