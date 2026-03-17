package com.bennybokki.frientrip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bennybokki.frientrip.data.Ride
import com.bennybokki.frientrip.data.RideRequest
import com.bennybokki.frientrip.data.SupplyItem
import com.bennybokki.frientrip.data.TripMember
import com.bennybokki.frientrip.data.TripRepository
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TripViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])
    private val repo = TripRepository()
    val currentUid: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val trip = repo.getTripDetails(tripId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val members = repo.getMembers(tripId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val supplyItems = repo.getSupplyItems(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rides = repo.getRides(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rideRequests = repo.getRideRequests(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Maps each member's uid → their computed share of the total trip cost.
     *
     * Algorithm: since everyone starts on night 1, a guest staying N nights is
     * present on nights 1..N. For each night we split its cost equally among all
     * guests who are still there (nightsStayed >= that night number).
     *
     *   nightly_cost = totalCost / totalNights
     *   member share = Σ (nightly_cost / guests_present_that_night)
     *                    for each night in 1..member.nightsStayed
     *
     * Members with nightsStayed == 0 owe $0 until they set their nights.
     */
    val memberCosts: kotlinx.coroutines.flow.StateFlow<Map<String, Double>> =
        combine(members, trip) { memberList, tripData ->
            computeCostSplit(memberList, tripData?.totalNights ?: 0, tripData?.totalCost ?: 0.0)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun computeCostSplit(
        members: List<TripMember>,
        totalNights: Int,
        totalCost: Double
    ): Map<String, Double> {
        if (totalNights <= 0 || totalCost <= 0.0 || members.isEmpty()) {
            return members.associate { it.uid to 0.0 }
        }
        val nightlyCost = totalCost / totalNights
        return members.associate { member ->
            val share = (1..member.nightsStayed).sumOf { night ->
                val presentCount = members.count { it.nightsStayed >= night }
                if (presentCount > 0) nightlyCost / presentCount else 0.0
            }
            member.uid to share
        }
    }

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _saveComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveComplete = _saveComplete.asSharedFlow()

    // ─── Members ──────────────────────────────────────────────────────────────

    fun updateNights(member: TripMember, nights: Int) = viewModelScope.launch {
        repo.updateMember(tripId, member.copy(nightsStayed = nights))
    }

    fun addPayment(member: TripMember, amount: Double) = viewModelScope.launch {
        repo.updateMember(tripId, member.copy(amountPaid = member.amountPaid + amount))
    }

    // ─── Supplies ─────────────────────────────────────────────────────────────

    fun addSupplyItem(name: String, category: String, quantity: String) = viewModelScope.launch {
        try {
            repo.addSupplyItem(tripId, name, category, quantity)
        } catch (e: Exception) {
            Log.e("TripViewModel", "addSupplyItem failed", e)
        }
    }

    fun reorderSupplyItems(category: String, reorderedItems: List<SupplyItem>) = viewModelScope.launch {
        try {
            repo.updateSupplyItems(tripId, reorderedItems)
        } catch (e: Exception) {
            Log.e("TripViewModel", "reorderSupplyItems failed", e)
        }
    }

    fun claimSupplyItem(item: SupplyItem, member: TripMember, quantity: String = "") = viewModelScope.launch {
        try {
            repo.updateSupplyItem(tripId, item.addClaim(member, quantity))
        } catch (e: Exception) {
            Log.e("TripViewModel", "claimSupplyItem failed", e)
        }
    }

    fun unclaimSupplyItem(item: SupplyItem, uid: String, displayName: String) = viewModelScope.launch {
        try {
            repo.updateSupplyItem(tripId, item.removeClaim(uid, displayName))
        } catch (e: Exception) {
            Log.e("TripViewModel", "unclaimSupplyItem failed", e)
        }
    }

    fun deleteSupplyItem(item: SupplyItem) = viewModelScope.launch {
        try {
            repo.deleteSupplyItem(tripId, item.id)
        } catch (e: Exception) {
            Log.e("TripViewModel", "deleteSupplyItem failed", e)
        }
    }

    // ─── Trip ──────────────────────────────────────────────────────────────────

    fun renameTrip(name: String) = viewModelScope.launch {
        repo.renameTripName(tripId, name)
    }

    // ─── House Details ─────────────────────────────────────────────────────────

    fun saveHouseDetails(url: String, nights: Int, cost: Double, checkInMillis: Long, checkOutMillis: Long) = viewModelScope.launch {
        _isSaving.value = true
        repo.saveHouseDetails(
            tripId = tripId,
            url = url,
            nights = nights,
            cost = cost,
            checkInMillis = checkInMillis,
            checkOutMillis = checkOutMillis,
            currentURL = trip.value?.houseURL,
            currentThumbnailURL = trip.value?.thumbnailURL
        )
        _isSaving.value = false
        _saveComplete.emit(Unit)
    }

    // ─── Rides ────────────────────────────────────────────────────────────────

    fun addRide(
        vehicleEmoji: String,
        vehicleLabel: String,
        departureLocation: String,
        totalSeats: Int,
        departureTime: Long,
        returnTime: Long,
        notes: String
    ) = viewModelScope.launch {
        try {
            val member = members.value.find { it.uid == currentUid } ?: return@launch
            val ride = Ride(
                driverUid = currentUid,
                driverName = member.displayName,
                vehicleEmoji = vehicleEmoji,
                vehicleLabel = vehicleLabel,
                departureLocation = departureLocation,
                totalSeats = totalSeats,
                departureTime = departureTime,
                returnTime = returnTime,
                notes = notes
            )
            repo.addRide(tripId, ride)
        } catch (e: Exception) {
            Log.e("TripViewModel", "addRide failed", e)
        }
    }

    fun claimSeat(rideId: String) = viewModelScope.launch {
        try {
            val member = members.value.find { it.uid == currentUid } ?: return@launch
            repo.claimSeat(tripId, rideId, currentUid, member.displayName)
            repo.removeRideRequest(tripId, currentUid)
        } catch (e: Exception) {
            Log.e("TripViewModel", "claimSeat failed", e)
        }
    }

    fun unclaimSeat(rideId: String) = viewModelScope.launch {
        try {
            val member = members.value.find { it.uid == currentUid } ?: return@launch
            repo.unclaimSeat(tripId, rideId, currentUid, member.displayName)
        } catch (e: Exception) {
            Log.e("TripViewModel", "unclaimSeat failed", e)
        }
    }

    fun updateRide(
        rideId: String,
        vehicleEmoji: String,
        vehicleLabel: String,
        departureLocation: String,
        totalSeats: Int,
        departureTime: Long,
        returnTime: Long,
        notes: String
    ) = viewModelScope.launch {
        try {
            repo.updateRide(tripId, rideId, mapOf(
                "vehicleEmoji" to vehicleEmoji,
                "vehicleLabel" to vehicleLabel,
                "departureLocation" to departureLocation,
                "totalSeats" to totalSeats,
                "departureTime" to departureTime,
                "returnTime" to returnTime,
                "notes" to notes
            ))
        } catch (e: Exception) {
            Log.e("TripViewModel", "updateRide failed", e)
        }
    }

    fun deleteRide(rideId: String) = viewModelScope.launch {
        try {
            repo.deleteRide(tripId, rideId)
        } catch (e: Exception) {
            Log.e("TripViewModel", "deleteRide failed", e)
        }
    }

    fun requestRide(notes: String = "") = viewModelScope.launch {
        try {
            val member = members.value.find { it.uid == currentUid } ?: return@launch
            repo.addRideRequest(tripId, RideRequest(uid = currentUid, displayName = member.displayName, notes = notes))
        } catch (e: Exception) {
            Log.e("TripViewModel", "requestRide failed", e)
        }
    }

    fun cancelRideRequest() = viewModelScope.launch {
        try {
            repo.removeRideRequest(tripId, currentUid)
        } catch (e: Exception) {
            Log.e("TripViewModel", "cancelRideRequest failed", e)
        }
    }

    fun canEditRide(ride: Ride): Boolean =
        currentUid == ride.driverUid || currentUid == (trip.value?.ownerId ?: "")

    // ─── Invites ──────────────────────────────────────────────────────────────

    fun inviteByEmail(email: String) = viewModelScope.launch {
        repo.inviteByEmail(tripId, email)
    }

    fun cancelInvite(email: String) = viewModelScope.launch {
        repo.cancelInvite(tripId, email)
    }
}
