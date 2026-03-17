package com.bennybokki.frientrip.auth

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bennybokki.frientrip.data.UserRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    data class LoggedIn(val uid: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    init {
        val current = auth.currentUser
        _authState.value = if (current != null) AuthState.LoggedIn(current.uid) else AuthState.LoggedOut
        _isAdmin.value = UserRepository.isAdminEmail(current?.email ?: "")

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _authState.value = if (user != null) AuthState.LoggedIn(user.uid) else AuthState.LoggedOut
            _isAdmin.value = UserRepository.isAdminEmail(user?.email ?: "")
        }
    }

    fun clearError() { _error.value = null }

    fun signInWithEmail(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: run {
                _authState.value = AuthState.LoggedOut
                return@launch
            }
            userRepository.ensureRoleSet(user.uid, user.email ?: "")
            val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.uid).get().await()
            val profile = com.bennybokki.frientrip.data.UserProfile(
                uid = user.uid,
                displayName = doc.getString("displayName") ?: user.displayName ?: "",
                email = user.email ?: "",
                avatarSeed = doc.getLong("avatarSeed") ?: 0L
            )
            userRepository.checkAndAcceptPendingInvites(
                email = user.email ?: "",
                uid = user.uid,
                displayName = profile.displayName,
                avatarSeed = profile.avatarSeed
            )
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Sign-in failed: ${e::class.simpleName} — ${e.message}", e)
            _error.value = e.message ?: "Sign in failed"
            _authState.value = if (auth.currentUser != null) AuthState.LoggedIn(auth.currentUser!!.uid) else AuthState.LoggedOut
        }
    }

    fun registerWithEmail(email: String, password: String, displayName: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: run {
                _authState.value = AuthState.LoggedOut
                return@launch
            }
            userRepository.createUserProfile(user.uid, displayName, email)
            // Fetch the generated avatar seed to pass to invite acceptance
            val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.uid).get().await()
            val avatarSeed = doc.getLong("avatarSeed") ?: 0L
            userRepository.checkAndAcceptPendingInvites(email, user.uid, displayName, avatarSeed)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Registration failed: ${e::class.simpleName} — ${e.message}", e)
            _error.value = e.message ?: "Registration failed"
            _authState.value = if (auth.currentUser != null) AuthState.LoggedIn(auth.currentUser!!.uid) else AuthState.LoggedOut
        }
    }

    fun signInWithGoogle(context: Context, webClientId: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            val credentialManager = CredentialManager.create(context)
            val googleIdToken = getGoogleIdToken(credentialManager, context, webClientId)

            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val result = auth.signInWithCredential(firebaseCredential).await()
            val user = result.user ?: return@launch
            val isNewUser = result.additionalUserInfo?.isNewUser == true

            if (isNewUser) {
                userRepository.createUserProfile(
                    uid = user.uid,
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User",
                    email = user.email ?: ""
                )
            } else {
                userRepository.ensureRoleSet(user.uid, user.email ?: "")
            }
            val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(user.uid).get().await()
            val avatarSeed = doc.getLong("avatarSeed") ?: 0L
            val displayNameFinal = doc.getString("displayName") ?: user.displayName ?: ""
            userRepository.checkAndAcceptPendingInvites(
                email = user.email ?: "",
                uid = user.uid,
                displayName = displayNameFinal,
                avatarSeed = avatarSeed
            )
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Google sign-in failed: ${e::class.simpleName} — ${e.message}", e)
            _error.value = "Google sign-in failed: ${e::class.simpleName}: ${e.message}"
            _authState.value = AuthState.LoggedOut
        }
    }

    /**
     * Two-step credential fetch:
     * 1. Try accounts already authorized with this app (faster, no account picker).
     * 2. Fall back to showing all Google accounts on the device.
     */
    private suspend fun getGoogleIdToken(
        credentialManager: CredentialManager,
        context: Context,
        webClientId: String
    ): String {
        // Step 1 — previously authorized accounts
        try {
            val authorizedOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(authorizedOption)
                .build()
            val response = credentialManager.getCredential(context, request)
            return GoogleIdTokenCredential.createFrom(response.credential.data).idToken
        } catch (e: NoCredentialException) {
            Log.d("AuthViewModel", "No previously authorized accounts, falling back to all accounts")
        }

        // Step 2 — all Google accounts on the device
        val allAccountsOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(allAccountsOption)
            .build()
        val response = credentialManager.getCredential(context, request)
        return GoogleIdTokenCredential.createFrom(response.credential.data).idToken
    }

    fun sendPasswordReset(email: String, onResult: (success: Boolean) -> Unit) =
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                onResult(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Password reset failed: ${e.message}", e)
                onResult(false)
            }
        }

    fun signOut() {
        auth.signOut()
    }
}
