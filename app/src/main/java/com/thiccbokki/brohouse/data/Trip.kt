package com.thiccbokki.brohouse.data

data class Trip(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val houseURL: String = "",
    val thumbnailURL: String? = null,
    val totalNights: Int = 0,
    val totalCost: Double = 0.0,
    val memberIds: List<String> = emptyList(),
    val pendingInviteEmails: List<String> = emptyList()
)
