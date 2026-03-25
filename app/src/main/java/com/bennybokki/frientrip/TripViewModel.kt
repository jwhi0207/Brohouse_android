package com.bennybokki.frientrip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bennybokki.frientrip.data.CostCalculator
import com.bennybokki.frientrip.data.Ride
import com.bennybokki.frientrip.data.RideRequest
import com.bennybokki.frientrip.data.SharedExpense
import com.bennybokki.frientrip.data.SupplyItem
import com.bennybokki.frientrip.data.TripHistoryEvent
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

    private var backfillAttempted = false

    init {
        viewModelScope.launch {
            trip.collect { t ->
                if (t != null && t.inviteCode == null && !backfillAttempted) {
                    backfillAttempted = true
                    try {
                        repo.regenerateInviteCode(tripId)
                    } catch (e: Exception) {
                        Log.e("TripViewModel", "Invite code backfill failed", e)
                    }
                }
            }
        }
    }

    val members = repo.getMembers(tripId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val supplyItems = repo.getSupplyItems(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rides = repo.getRides(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rideRequests = repo.getRideRequests(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses = repo.getExpenses(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: kotlinx.coroutines.flow.StateFlow<List<TripHistoryEvent>> =
        repo.getTripHistory(tripId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Maps each member's uid → their computed share of the total trip cost
     * including house cost + all approved shared expenses.
     */
    val memberCosts: kotlinx.coroutines.flow.StateFlow<Map<String, Double>> =
        combine(members, trip, expenses) { memberList, tripData, expenseList ->
            val activeMembers = memberList.filter { !it.isDeactivated }
            val approvedExpenses = expenseList.filter { it.approved }
            CostCalculator.computeTotalShares(
                activeMembers,
                tripData?.totalNights ?: 0,
                tripData?.totalCost ?: 0.0,
                approvedExpenses
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * House-only cost split (for payment breakdown display).
     */
    val houseCosts: kotlinx.coroutines.flow.StateFlow<Map<String, Double>> =
        combine(members, trip) { memberList, tripData ->
            val activeMembers = memberList.filter { !it.isDeactivated }
            CostCalculator.computeCostSplit(activeMembers, tripData?.totalNights ?: 0, tripData?.totalCost ?: 0.0)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _saveComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveComplete = _saveComplete.asSharedFlow()

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage = _errorMessage.asSharedFlow()

    private fun logHistory(category: String, description: String) {
        viewModelScope.launch {
            try { repo.logTripHistory(tripId, category, description) }
            catch (e: Exception) { Log.e("TripViewModel", "logHistory failed", e) }
        }
    }

    private fun actorName() =
        members.value.find { it.uid == currentUid }?.displayName ?: "Someone"

    // ─── Members ──────────────────────────────────────────────────────────────

    fun updateNights(member: TripMember, nights: Int) = viewModelScope.launch {
        repo.updateMember(tripId, member.copy(nightsStayed = nights))
    }

    fun addPayment(member: TripMember, amount: Double) = viewModelScope.launch {
        repo.updateMember(tripId, member.copy(amountPaid = member.amountPaid + amount))
    }

    fun submitPaymentForReview(member: TripMember, amount: Double) = viewModelScope.launch {
        repo.submitPendingPayment(tripId, member.uid, amount, member.displayName)
        logHistory("payments", "${member.displayName} submitted a $${String.format("%.2f", amount)} payment")
    }

    fun approvePendingPayment(member: TripMember) = viewModelScope.launch {
        val adminName = members.value.find { it.uid == currentUid }?.displayName ?: "Trip Manager"
        repo.approvePendingPayment(tripId, member, adminName)
        logHistory("payments", "$adminName approved ${member.displayName}'s $${String.format("%.2f", member.pendingPaymentAmount)} payment")
    }

    fun rejectPendingPayment(member: TripMember) = viewModelScope.launch {
        val adminName = members.value.find { it.uid == currentUid }?.displayName ?: "Trip Manager"
        repo.rejectPendingPayment(tripId, member.uid, member.pendingPaymentAmount, adminName)
        logHistory("payments", "$adminName rejected ${member.displayName}'s $${String.format("%.2f", member.pendingPaymentAmount)} payment")
    }

    // ─── Supplies ─────────────────────────────────────────────────────────────

    fun addSupplyItem(name: String, category: String, quantity: String) = viewModelScope.launch {
        try {
            repo.addSupplyItem(tripId, name, category, quantity)
            val qty = if (quantity.isNotBlank()) " ($quantity)" else ""
            logHistory("supplies", "${actorName()} added $name$qty")
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
            val qty = if (quantity.isNotBlank()) " ($quantity)" else ""
            logHistory("supplies", "${member.displayName} claimed ${item.name}$qty")
        } catch (e: Exception) {
            Log.e("TripViewModel", "claimSupplyItem failed", e)
        }
    }

    fun unclaimSupplyItem(item: SupplyItem, uid: String, displayName: String) = viewModelScope.launch {
        try {
            repo.updateSupplyItem(tripId, item.removeClaim(uid, displayName))
            logHistory("supplies", "$displayName unclaimed ${item.name}")
        } catch (e: Exception) {
            Log.e("TripViewModel", "unclaimSupplyItem failed", e)
        }
    }

    fun deleteSupplyItem(item: SupplyItem) = viewModelScope.launch {
        try {
            repo.deleteSupplyItem(tripId, item.id)
            logHistory("supplies", "${actorName()} removed ${item.name}")
        } catch (e: Exception) {
            Log.e("TripViewModel", "deleteSupplyItem failed", e)
        }
    }

    // ─── Trip ──────────────────────────────────────────────────────────────────

    fun renameTrip(name: String) = viewModelScope.launch {
        repo.renameTripName(tripId, name)
    }

    // ─── House Details ─────────────────────────────────────────────────────────

    fun saveHouseDetails(url: String, address: String, nights: Int, cost: Double, checkInMillis: Long, checkOutMillis: Long) = viewModelScope.launch {
        _isSaving.value = true
        repo.saveHouseDetails(
            tripId = tripId,
            url = url,
            address = address,
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

    // ─── Expenses ────────────────────────────────────────────────────────────

    fun submitExpense(
        description: String,
        amount: Double,
        splitMethod: String,
        category: String = "misc",
        linkedSupplyId: String? = null
    ) = viewModelScope.launch {
        try {
            val member = members.value.find { it.uid == currentUid }
            if (member == null) {
                _errorMessage.tryEmit("Could not find your member profile")
                return@launch
            }
            val expense = SharedExpense(
                description = description,
                amount = amount,
                category = category,
                splitMethod = splitMethod,
                submittedByUid = currentUid,
                submittedByName = member.displayName,
                approved = false,
                linkedSupplyId = linkedSupplyId,
                createdAt = System.currentTimeMillis()
            )
            repo.addExpense(tripId, expense)
            logHistory("expenses", "${member.displayName} submitted '$description' ($${String.format("%.2f", amount)})")
        } catch (e: Exception) {
            Log.e("TripViewModel", "submitExpense failed", e)
            _errorMessage.tryEmit("Failed to submit expense: ${e.message}")
        }
    }

    fun approveExpense(expenseId: String) = viewModelScope.launch {
        try {
            val expense = expenses.value.find { it.id == expenseId }
            repo.approveExpense(tripId, expenseId)
            if (expense != null) {
                logHistory("expenses", "${actorName()} approved '${expense.description}' ($${String.format("%.2f", expense.amount)})")
            }
        } catch (e: Exception) {
            Log.e("TripViewModel", "approveExpense failed", e)
            _errorMessage.tryEmit("Failed to approve expense: ${e.message}")
        }
    }

    fun deleteExpense(expenseId: String, reason: String = "removed") = viewModelScope.launch {
        try {
            val expense = expenses.value.find { it.id == expenseId }
            repo.deleteExpense(tripId, expenseId)
            if (expense != null) {
                logHistory("expenses", "${actorName()} $reason '${expense.description}' ($${String.format("%.2f", expense.amount)})")
            }
        } catch (e: Exception) {
            Log.e("TripViewModel", "deleteExpense failed", e)
            _errorMessage.tryEmit("Failed to delete expense: ${e.message}")
        }
    }

    // ─── Member deactivation ─────────────────────────────────────────────────

    fun deactivateMember(uid: String) = viewModelScope.launch {
        try {
            val displayName = members.value.find { it.uid == uid }?.displayName ?: ""
            repo.deactivateMember(tripId, uid, displayName)
        } catch (e: Exception) {
            Log.e("TripViewModel", "deactivateMember failed", e)
            _errorMessage.tryEmit("Failed to deactivate member: ${e.message}")
        }
    }

    fun reactivateMember(uid: String) = viewModelScope.launch {
        try {
            repo.reactivateMember(tripId, uid)
        } catch (e: Exception) {
            Log.e("TripViewModel", "reactivateMember failed", e)
            _errorMessage.tryEmit("Failed to reactivate member: ${e.message}")
        }
    }

    // ─── Invites ──────────────────────────────────────────────────────────────

    fun inviteByEmail(email: String) = viewModelScope.launch {
        repo.inviteByEmail(tripId, email.trim().lowercase())
    }

    fun cancelInvite(email: String) = viewModelScope.launch {
        repo.cancelInvite(tripId, email)
    }

    fun toggleInviteCode(enabled: Boolean) = viewModelScope.launch {
        try {
            repo.setInviteCodeEnabled(tripId, enabled)
        } catch (e: Exception) {
            Log.e("TripViewModel", "toggleInviteCode failed", e)
        }
    }

    fun regenerateInviteCode() = viewModelScope.launch {
        try {
            repo.regenerateInviteCode(tripId)
        } catch (e: Exception) {
            Log.e("TripViewModel", "regenerateInviteCode failed", e)
        }
    }
}
