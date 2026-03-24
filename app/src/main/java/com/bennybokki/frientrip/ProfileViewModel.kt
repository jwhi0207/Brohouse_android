package com.bennybokki.frientrip

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bennybokki.frientrip.data.UserProfile
import com.bennybokki.frientrip.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val userRepo = UserRepository()

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile = _profile.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _saveComplete = MutableSharedFlow<Unit>()
    val saveComplete = _saveComplete.asSharedFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError = _saveError.asStateFlow()

    fun clearError() { _saveError.value = null }

    init {
        val uid = currentUid
        if (uid.isNotEmpty()) {
            viewModelScope.launch {
                userRepo.getUserProfile(uid).collect { _profile.value = it }
            }
        }
    }

    fun saveProfile(displayName: String, avatarSeed: Long, avatarColor: Int) = viewModelScope.launch {
        val uid = currentUid
        if (uid.isEmpty() || displayName.isBlank()) return@launch
        _isSaving.value = true
        try {
            _saveError.value = null
            userRepo.updateProfile(uid, displayName.trim(), avatarSeed, avatarColor)
            _saveComplete.emit(Unit)
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Save failed: ${e.message}", e)
            _saveError.value = "Save failed. Please try again."
        } finally {
            _isSaving.value = false
        }
    }
}
