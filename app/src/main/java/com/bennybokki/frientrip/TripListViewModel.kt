package com.bennybokki.frientrip

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bennybokki.frientrip.data.Trip
import com.bennybokki.frientrip.data.TripRepository
import com.bennybokki.frientrip.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TripListViewModel(application: Application) : AndroidViewModel(application) {

    private val tripRepo = TripRepository()
    private val userRepo = UserRepository()
    private val currentUid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val currentEmail: String get() = FirebaseAuth.getInstance().currentUser?.email ?: ""

    val trips = tripRepo.getUserTrips(currentUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingInviteTrips = tripRepo.getPendingInviteTrips(currentEmail)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _joinCodeError = MutableStateFlow<String?>(null)
    val joinCodeError = _joinCodeError.asStateFlow()

    private val _joinCodeLoading = MutableStateFlow(false)
    val joinCodeLoading = _joinCodeLoading.asStateFlow()

    fun joinByCode(code: String) = viewModelScope.launch {
        _joinCodeError.value = null
        _joinCodeLoading.value = true
        try {
            val trip = tripRepo.findTripByInviteCode(code)
            if (trip == null) {
                _joinCodeError.value = "Invalid or disabled invite code"
                return@launch
            }
            val uid = currentUid
            if (uid in trip.memberIds) {
                _joinCodeError.value = "You're already a member of this trip"
                return@launch
            }
            val user = FirebaseAuth.getInstance().currentUser ?: return@launch
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users").document(user.uid).get().await()
            val displayName = userDoc.getString("displayName") ?: user.displayName ?: "Unknown"
            val email = user.email ?: ""
            val avatarSeed = userDoc.getLong("avatarSeed") ?: 0L
            tripRepo.joinTripByCode(trip.id, uid, displayName, email, avatarSeed)
        } catch (e: IllegalStateException) {
            _joinCodeError.value = e.message
        } catch (e: Exception) {
            Log.e("TripListViewModel", "joinByCode failed: ${e.message}", e)
            _joinCodeError.value = "Failed to join trip. Please try again."
        } finally {
            _joinCodeLoading.value = false
        }
    }

    fun clearJoinCodeError() {
        _joinCodeError.value = null
    }

    fun acceptInvite(tripId: String) = viewModelScope.launch {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return@launch
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users").document(user.uid).get().await()
            val displayName = userDoc.getString("displayName") ?: user.displayName ?: "Unknown"
            val avatarSeed = userDoc.getLong("avatarSeed") ?: 0L
            userRepo.checkAndAcceptPendingInvites(
                email = user.email ?: "",
                uid = user.uid,
                displayName = displayName,
                avatarSeed = avatarSeed
            )
        } catch (e: Exception) {
            Log.e("TripListViewModel", "acceptInvite failed: ${e.message}", e)
        }
    }

    fun createTrip(name: String) = viewModelScope.launch {
        try {
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
        } catch (e: Exception) {
            Log.e("TripListViewModel", "createTrip failed: ${e.message}", e)
        }
    }
}
