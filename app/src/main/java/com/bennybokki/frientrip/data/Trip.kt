package com.bennybokki.frientrip.data

data class Trip(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val houseURL: String = "",
    val thumbnailURL: String? = null,
    val address: String = "",
    val totalNights: Int = 0,
    val totalCost: Double = 0.0,
    val checkInMillis: Long = 0L,
    val checkOutMillis: Long = 0L,
    val memberIds: List<String> = emptyList(),
    val deactivatedMemberIds: List<String> = emptyList(),
    val pendingInviteEmails: List<String> = emptyList(),
    val inviteCode: String? = null,
    val inviteCodeEnabled: Boolean = true,
    val description: String = "",
    val emoji: String = ""
)
