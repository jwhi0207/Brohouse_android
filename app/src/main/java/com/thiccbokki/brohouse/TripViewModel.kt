package com.thiccbokki.brohouse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.thiccbokki.brohouse.data.SupplyItem
import com.thiccbokki.brohouse.data.TripMember
import com.thiccbokki.brohouse.data.TripRepository
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val members = repo.getMembers(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val supplyItems = repo.getSupplyItems(tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _saveComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveComplete = _saveComplete.asSharedFlow()

    // ─── Members ──────────────────────────────────────────────────────────────

    fun updateNights(member: TripMember, nights: Int) = viewModelScope.launch {
        repo.updateMember(tripId, member.copy(nightsStayed = nights))
    }

    fun addPayment(member: TripMember, amount: Double) = viewModelScope.launch {
        repo.updateMember(tripId, member.copy(moneyOwed = maxOf(0.0, member.moneyOwed - amount)))
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

    // ─── House Details ─────────────────────────────────────────────────────────

    fun saveHouseDetails(url: String, nights: Int, cost: Double) = viewModelScope.launch {
        _isSaving.value = true
        repo.saveHouseDetails(
            tripId = tripId,
            url = url,
            nights = nights,
            cost = cost,
            currentURL = trip.value?.houseURL
        )
        _isSaving.value = false
        _saveComplete.emit(Unit)
    }

    // ─── Invites ──────────────────────────────────────────────────────────────

    fun inviteByEmail(email: String) = viewModelScope.launch {
        repo.inviteByEmail(tripId, email)
    }

    fun cancelInvite(email: String) = viewModelScope.launch {
        repo.cancelInvite(tripId, email)
    }
}
