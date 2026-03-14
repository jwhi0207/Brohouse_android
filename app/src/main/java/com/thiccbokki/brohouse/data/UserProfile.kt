package com.thiccbokki.brohouse.data

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarSeed: Long = 0L,
    val role: String = "user"
) {
    val isAdmin: Boolean get() = role == "admin"
}
