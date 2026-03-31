package com.bennybokki.frientrip.data

data class TripMember(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarSeed: Long = 0L,
    val avatarColor: Int = 0,
    val nightsStayed: Int = 0,
    val amountPaid: Double = 0.0,  // amount paid toward their computed share
    val pendingPaymentAmount: Double = 0.0,
    val pendingPaymentStatus: String = "none", // "none" | "pending" | "rejected"
    val status: String = "active", // "active" | "deactivated"
    val isGuest: Boolean = false
) {
    val isDeactivated: Boolean get() = status == "deactivated"
}
