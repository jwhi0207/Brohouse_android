package com.thiccbokki.brohouse.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class TripRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val tripsCollection = db.collection("trips")

    // ─── Trip CRUD ───────────────────────────────────────────────────────────

    suspend fun createTrip(
        name: String,
        ownerId: String,
        ownerDisplayName: String,
        ownerEmail: String,
        ownerAvatarSeed: Long
    ): String {
        val tripRef = tripsCollection.document()
        val tripId = tripRef.id
        val tripData = mapOf(
            "name" to name,
            "ownerId" to ownerId,
            "houseURL" to "",
            "thumbnailURL" to null,
            "totalNights" to 0,
            "totalCost" to 0.0,
            "memberIds" to listOf(ownerId),
            "pendingInviteEmails" to emptyList<String>()
        )
        val memberData = mapOf(
            "uid" to ownerId,
            "displayName" to ownerDisplayName,
            "email" to ownerEmail,
            "avatarSeed" to ownerAvatarSeed,
            "nightsStayed" to 0,
            "amountPaid" to 0.0
        )
        db.runBatch { batch ->
            batch.set(tripRef, tripData)
            batch.set(tripRef.collection("members").document(ownerId), memberData)
        }.await()
        return tripId
    }

    fun getUserTrips(uid: String): Flow<List<Trip>> = callbackFlow {
        val listener = tripsCollection
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snap, _ ->
                val trips = snap?.documents?.map { doc ->
                    Trip(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        ownerId = doc.getString("ownerId") ?: "",
                        houseURL = doc.getString("houseURL") ?: "",
                        thumbnailURL = doc.getString("thumbnailURL"),
                        totalNights = (doc.getLong("totalNights") ?: 0L).toInt(),
                        totalCost = doc.getDouble("totalCost") ?: 0.0,
                        memberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        pendingInviteEmails = (doc.get("pendingInviteEmails") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                } ?: emptyList()
                trySend(trips)
            }
        awaitClose { listener.remove() }
    }

    fun getTripDetails(tripId: String): Flow<Trip?> = callbackFlow {
        val listener = tripsCollection.document(tripId).addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                trySend(
                    Trip(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        ownerId = doc.getString("ownerId") ?: "",
                        houseURL = doc.getString("houseURL") ?: "",
                        thumbnailURL = doc.getString("thumbnailURL"),
                        totalNights = (doc.getLong("totalNights") ?: 0L).toInt(),
                        totalCost = doc.getDouble("totalCost") ?: 0.0,
                        memberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        pendingInviteEmails = (doc.get("pendingInviteEmails") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                )
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    // ─── Members ─────────────────────────────────────────────────────────────

    fun getMembers(tripId: String): Flow<List<TripMember>> = callbackFlow {
        val listener = tripsCollection.document(tripId)
            .collection("members")
            .addSnapshotListener { snap, _ ->
                val members = snap?.documents?.map { doc ->
                    TripMember(
                        uid = doc.getString("uid") ?: doc.id,
                        displayName = doc.getString("displayName") ?: "",
                        email = doc.getString("email") ?: "",
                        avatarSeed = doc.getLong("avatarSeed") ?: 0L,
                        nightsStayed = (doc.getLong("nightsStayed") ?: 0L).toInt(),
                        amountPaid = doc.getDouble("amountPaid") ?: 0.0
                    )
                }?.sortedBy { it.displayName } ?: emptyList()
                trySend(members)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateMember(tripId: String, member: TripMember) {
        tripsCollection.document(tripId)
            .collection("members")
            .document(member.uid)
            .update(
                mapOf(
                    "nightsStayed" to member.nightsStayed,
                    "amountPaid" to member.amountPaid
                )
            ).await()
    }

    // ─── Supplies ─────────────────────────────────────────────────────────────

    fun getSupplyItems(tripId: String): Flow<List<SupplyItem>> = callbackFlow {
        val listener = tripsCollection.document(tripId)
            .collection("supplies")
            .orderBy("category")
            .orderBy("sortOrder")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("TripRepository", "getSupplyItems listener failed", error)
                }
                val items = snap?.documents?.map { doc ->
                    SupplyItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        category = doc.getString("category") ?: "",
                        quantity = doc.getString("quantity") ?: "",
                        claimedByUids = (doc.get("claimedByUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        claimedByName = doc.getString("claimedByName"),
                        sortOrder = (doc.getLong("sortOrder") ?: 0L).toInt()
                    )
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getMaxSortOrder(tripId: String, category: String): Int {
        val snap = tripsCollection.document(tripId)
            .collection("supplies")
            .whereEqualTo("category", category)
            .orderBy("sortOrder", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        return (snap.documents.firstOrNull()?.getLong("sortOrder") ?: -1L).toInt()
    }

    suspend fun addSupplyItem(tripId: String, name: String, category: String, quantity: String) {
        val nextOrder = getMaxSortOrder(tripId, category) + 1
        val data = mapOf(
            "name" to name,
            "category" to category,
            "quantity" to quantity,
            "claimedByUids" to emptyList<String>(),
            "claimedByName" to null,
            "sortOrder" to nextOrder
        )
        tripsCollection.document(tripId).collection("supplies").add(data).await()
    }

    suspend fun updateSupplyItem(tripId: String, item: SupplyItem) {
        val data = mapOf(
            "name" to item.name,
            "category" to item.category,
            "quantity" to item.quantity,
            "claimedByUids" to item.claimedByUids,
            "claimedByName" to item.claimedByName,
            "sortOrder" to item.sortOrder
        )
        tripsCollection.document(tripId).collection("supplies").document(item.id).update(data).await()
    }

    suspend fun updateSupplyItems(tripId: String, items: List<SupplyItem>) {
        val suppliesRef = tripsCollection.document(tripId).collection("supplies")
        db.runBatch { batch ->
            items.forEachIndexed { index, item ->
                batch.update(suppliesRef.document(item.id), "sortOrder", index)
            }
        }.await()
    }

    suspend fun deleteSupplyItem(tripId: String, itemId: String) {
        tripsCollection.document(tripId).collection("supplies").document(itemId).delete().await()
    }

    // ─── House Details ────────────────────────────────────────────────────────

    suspend fun saveHouseDetails(
        tripId: String,
        url: String,
        nights: Int,
        cost: Double,
        currentURL: String?
    ) {
        val urlChanged = url != (currentURL ?: "")
        val thumbnailURL: String? = if (urlChanged && url.isNotBlank()) {
            withContext(Dispatchers.IO) { fetchAndUploadThumbnail(tripId, url) }
        } else {
            if (url.isBlank()) null else currentURL
        }

        tripsCollection.document(tripId).update(
            mapOf(
                "houseURL" to url,
                "totalNights" to nights,
                "totalCost" to cost,
                "thumbnailURL" to thumbnailURL
            )
        ).await()
    }

    private suspend fun fetchAndUploadThumbnail(tripId: String, urlString: String): String? {
        return try {
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible)")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val html = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val imageUrlString = extractOGImageURL(html) ?: return null
            val resolved = when {
                imageUrlString.startsWith("//") -> "https:$imageUrlString"
                imageUrlString.startsWith("http") -> imageUrlString
                else -> URL(URL(urlString), imageUrlString).toString()
            }

            val imgConn = URL(resolved).openConnection() as HttpURLConnection
            imgConn.connectTimeout = 10_000
            imgConn.readTimeout = 10_000
            val bytes = imgConn.inputStream.readBytes()
            imgConn.disconnect()

            // Upload to Firebase Storage
            val ref = storage.reference.child("thumbnails/$tripId.jpg")
            ref.putBytes(bytes).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun extractOGImageURL(html: String): String? {
        val patterns = listOf(
            Regex("""<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']""", RegexOption.IGNORE_CASE)
        )
        for (regex in patterns) {
            val match = regex.find(html)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    // ─── Invites ──────────────────────────────────────────────────────────────

    suspend fun inviteByEmail(tripId: String, email: String) {
        val tripRef = tripsCollection.document(tripId)
        val trip = tripRef.get().await()
        val current = (trip.get("pendingInviteEmails") as? List<*>)
            ?.filterIsInstance<String>() ?: emptyList()
        if (email !in current) {
            tripRef.update("pendingInviteEmails", current + email).await()
        }
    }

    suspend fun cancelInvite(tripId: String, email: String) {
        val tripRef = tripsCollection.document(tripId)
        val trip = tripRef.get().await()
        val current = (trip.get("pendingInviteEmails") as? List<*>)
            ?.filterIsInstance<String>() ?: emptyList()
        tripRef.update("pendingInviteEmails", current.filter { it != email }).await()
    }
}
