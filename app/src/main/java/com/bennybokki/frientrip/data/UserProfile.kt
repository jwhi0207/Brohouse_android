package com.bennybokki.frientrip.data

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarSeed: Long = 0L,
    val avatarColor: Int = 0,
    val role: String = "user"
) {
    val isAdmin: Boolean get() = role == "admin"
}
