package com.thiccbokki.brohouse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.thiccbokki.brohouse.data.SupplyItem
import com.thiccbokki.brohouse.data.TripMember
import com.thiccbokki.brohouse.data.TripRepository
import com.google.firebase.auth.FirebaseAuth
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
        repo.addSupplyItem(tripId, name, category, quantity)
    }

    fun reorderSupplyItems(category: String, reorderedItems: List<SupplyItem>) = viewModelScope.launch {
        repo.updateSupplyItems(tripId, reorderedItems)
    }

    fun claimSupplyItem(item: SupplyItem, member: TripMember, quantity: String = "") = viewModelScope.launch {
        repo.updateSupplyItem(tripId, item.addClaim(member, quantity))
    }

    fun unclaimSupplyItem(item: SupplyItem, uid: String, displayName: String) = viewModelScope.launch {
        repo.updateSupplyItem(tripId, item.removeClaim(uid, displayName))
    }

    fun deleteSupplyItem(item: SupplyItem) = viewModelScope.launch {
        repo.deleteSupplyItem(tripId, item.id)
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
