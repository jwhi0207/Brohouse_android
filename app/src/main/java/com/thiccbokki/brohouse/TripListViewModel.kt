package com.thiccbokki.brohouse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thiccbokki.brohouse.data.Trip
import com.thiccbokki.brohouse.data.TripRepository
import com.thiccbokki.brohouse.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TripListViewModel(application: Application) : AndroidViewModel(application) {

    private val tripRepo = TripRepository()
    private val currentUid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val trips = tripRepo.getUserTrips(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTrip(name: String) = viewModelScope.launch {
        val user = FirebaseAuth.getInstance().currentUser ?: return@launch
        val userDoc = FirebaseFirestore.getInstance()
            .collection("users").document(user.uid).get().await()
        val displayName = userDoc.getString("displayName") ?: user.displayName ?: "Unknown"
        val email = user.email ?: ""
        val avatarSeed = userDoc.getLong("avatarSeed") ?: 0L
        tripRepo.createTrip(
            name = name,
            ownerId = user.uid,
            ownerDisplayName = displayName,
            ownerEmail = email,
            ownerAvatarSeed = avatarSeed
        )
    }
}
