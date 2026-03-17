package com.bennybokki.frientrip.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = db.collection("users")
    private val tripsCollection = db.collection("trips")

    companion object {
        // TODO: Replace these with your two real admin email addresses
        private val ADMIN_EMAILS = setOf(
            "jwhi0207@gmail.com",
            "benjamincroberts@gmail.com"
        )

        fun isAdminEmail(email: String): Boolean =
            email.trim().lowercase() in ADMIN_EMAILS

        fun roleForEmail(email: String): String =
            if (isAdminEmail(email)) "admin" else "user"
    }

    suspend fun createUserProfile(uid: String, displayName: String, email: String) {
        val profile = mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "email" to email,
            "avatarSeed" to Random.nextLong(),
            "role" to roleForEmail(email)
        )
        usersCollection.document(uid).set(profile, SetOptions.merge()).await()
    }

    /**
     * Called on every login for existing users to ensure the role field is present
     * (back-fills users who registered before roles were added).
     */
    suspend fun ensureRoleSet(uid: String, email: String) {
        val doc = usersCollection.document(uid).get().await()
        if (doc.getString("role") == null) {
            usersCollection.document(uid)
                .update("role", roleForEmail(email))
                .await()
        }
    }

    fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = usersCollection.document(uid).addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) {
                trySend(
                    UserProfile(
                        uid = snap.getString("uid") ?: uid,
                        displayName = snap.getString("displayName") ?: "",
                        email = snap.getString("email") ?: "",
                        avatarSeed = snap.getLong("avatarSeed") ?: 0L,
                        role = snap.getString("role") ?: "user"
                    )
                )
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    /** On login/register, check if this user's email has any pending trip invites and accept them. */
    suspend fun checkAndAcceptPendingInvites(email: String, uid: String, displayName: String, avatarSeed: Long) {
        val normalizedEmail = email.trim().lowercase()
        val trips = tripsCollection
            .whereArrayContains("pendingInviteEmails", normalizedEmail)
            .get()
            .await()

        for (tripDoc in trips.documents) {
            val tripId = tripDoc.id
            val currentPending = (tripDoc.get("pendingInviteEmails") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
            val currentMembers = (tripDoc.get("memberIds") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()

            if (uid in currentMembers) continue

            db.runBatch { batch ->
                // Add uid to memberIds, remove email from pending
                batch.update(
                    tripsCollection.document(tripId),
                    mapOf(
                        "memberIds" to currentMembers + uid,
                        "pendingInviteEmails" to currentPending.filter { it != normalizedEmail }
                    )
                )
                // Create member subcollection doc
                batch.set(
                    tripsCollection.document(tripId).collection("members").document(uid),
                    mapOf(
                        "uid" to uid,
                        "displayName" to displayName,
                        "email" to email,
                        "avatarSeed" to avatarSeed,
                        "nightsStayed" to 0,
                        "amountPaid" to 0.0
                    )
                )
            }.await()
        }
    }
}
