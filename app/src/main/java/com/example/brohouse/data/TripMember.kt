package com.thiccbokki.brohouse.data

data class TripMember(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarSeed: Long = 0L,
    val nightsStayed: Int = 0,
    val moneyOwed: Double = 0.0
)
