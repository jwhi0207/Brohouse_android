package com.bennybokki.frientrip.data

data class TripMember(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarSeed: Long = 0L,
    val nightsStayed: Int = 0,
    val amountPaid: Double = 0.0   // amount paid toward their computed share
)
