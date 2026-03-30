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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TripCreationParams(
    val name: String,
    val houseURL: String = "",
    val address: String = "",
    val totalCost: Double = 0.0,
    val checkInMillis: Long = 0L,
    val checkOutMillis: Long = 0L,
    val description: String = "",
    val emoji: String = "",
    val inviteEmails: List<String> = emptyList()
)

class TripListViewModel(application: Application) : AndroidViewModel(application) {

    private val tripRepo = TripRepository()
    private val userRepo = UserRepository()
    private val currentUid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val currentEmail: String get() = FirebaseAuth.getInstance().currentUser?.email ?: ""

    val trips = tripRepo.getUserTrips(currentUid)
        .map { list ->
            val (withDate, withoutDate) = list.partition { it.checkInMillis > 0L }
            withDate.sortedBy { it.checkInMillis } +
                withoutDate.sortedBy { it.name.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingInviteTrips = tripRepo.getPendingInviteTrips(currentEmail)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _joinCodeError = MutableStateFlow<String?>(null)
    val joinCodeError = _joinCodeError.asStateFlow()

    private val _joinCodeLoading = MutableStateFlow(false)
    val joinCodeLoading = _joinCodeLoading.asStateFlow()

    private val _joinCodeSuccess = MutableStateFlow(false)
    val joinCodeSuccess = _joinCodeSuccess.asStateFlow()

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
            val avatarColor = userDoc.getLong("avatarColor")?.toInt() ?: 0
            tripRepo.joinTripByCode(trip.id, uid, displayName, email, avatarSeed, avatarColor)
            _joinCodeSuccess.value = true
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

    fun clearJoinCodeSuccess() {
        _joinCodeSuccess.value = false
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

    fun createTrip(params: TripCreationParams) = viewModelScope.launch {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return@launch
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users").document(user.uid).get().await()
            val displayName = userDoc.getString("displayName") ?: user.displayName ?: "Unknown"
            val email = user.email ?: ""
            val avatarSeed = userDoc.getLong("avatarSeed") ?: 0L
            val avatarColor = userDoc.getLong("avatarColor")?.toInt() ?: 0
            tripRepo.createTrip(
                name = params.name,
                ownerId = user.uid,
                ownerDisplayName = displayName,
                ownerEmail = email,
                ownerAvatarSeed = avatarSeed,
                ownerAvatarColor = avatarColor,
                houseURL = params.houseURL,
                address = params.address,
                totalCost = params.totalCost,
                checkInMillis = params.checkInMillis,
                checkOutMillis = params.checkOutMillis,
                description = params.description,
                emoji = params.emoji,
                pendingInviteEmails = params.inviteEmails
            )
        } catch (e: Exception) {
            Log.e("TripListViewModel", "createTrip failed: ${e.message}", e)
        }
    }
}
