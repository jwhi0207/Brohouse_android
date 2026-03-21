package com.bennybokki.frientrip.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

class TripRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val tripsCollection = db.collection("trips")

    companion object {
        private const val INVITE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

        fun generateInviteCode(): String {
            val chars = (1..8).map { INVITE_ALPHABET[Random.nextInt(INVITE_ALPHABET.length)] }
            return "${chars.subList(0, 4).joinToString("")}-${chars.subList(4, 8).joinToString("")}"
        }

        fun normalizeInviteCode(raw: String): String {
            val stripped = raw.uppercase().filter { it.isLetterOrDigit() }
            return if (stripped.length == 8) "${stripped.substring(0, 4)}-${stripped.substring(4)}" else stripped
        }
    }

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
            "address" to "",
            "totalNights" to 0,
            "totalCost" to 0.0,
            "memberIds" to listOf(ownerId),
            "pendingInviteEmails" to emptyList<String>(),
            "inviteCode" to generateInviteCode(),
            "inviteCodeEnabled" to true
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
                        address = doc.getString("address") ?: "",
                        totalNights = (doc.getLong("totalNights") ?: 0L).toInt(),
                        totalCost = doc.getDouble("totalCost") ?: 0.0,
                        checkInMillis = doc.getLong("checkInMillis") ?: 0L,
                        checkOutMillis = doc.getLong("checkOutMillis") ?: 0L,
                        memberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        pendingInviteEmails = (doc.get("pendingInviteEmails") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        inviteCode = doc.getString("inviteCode"),
                        inviteCodeEnabled = doc.getBoolean("inviteCodeEnabled") ?: true
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
                        address = doc.getString("address") ?: "",
                        totalNights = (doc.getLong("totalNights") ?: 0L).toInt(),
                        totalCost = doc.getDouble("totalCost") ?: 0.0,
                        checkInMillis = doc.getLong("checkInMillis") ?: 0L,
                        checkOutMillis = doc.getLong("checkOutMillis") ?: 0L,
                        memberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        pendingInviteEmails = (doc.get("pendingInviteEmails") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        inviteCode = doc.getString("inviteCode"),
                        inviteCodeEnabled = doc.getBoolean("inviteCodeEnabled") ?: true
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
                        amountPaid = doc.getDouble("amountPaid") ?: 0.0,
                        pendingPaymentAmount = doc.getDouble("pendingPaymentAmount") ?: 0.0,
                        pendingPaymentStatus = doc.getString("pendingPaymentStatus") ?: "none"
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

    suspend fun submitPendingPayment(tripId: String, uid: String, amount: Double, actorName: String) {
        val memberRef = tripsCollection.document(tripId).collection("members").document(uid)
        memberRef.update(
            mapOf(
                "pendingPaymentAmount" to amount,
                "pendingPaymentStatus" to "pending"
            )
        ).await()
        logPaymentEvent(tripId, uid, "submitted", amount, actorName)
    }

    suspend fun approvePendingPayment(tripId: String, member: TripMember, actorName: String) {
        val newAmountPaid = member.amountPaid + member.pendingPaymentAmount
        tripsCollection.document(tripId).collection("members").document(member.uid)
            .update(
                mapOf(
                    "amountPaid" to newAmountPaid,
                    "pendingPaymentAmount" to 0.0,
                    "pendingPaymentStatus" to "none"
                )
            ).await()
        logPaymentEvent(tripId, member.uid, "approved", member.pendingPaymentAmount, actorName)
    }

    suspend fun rejectPendingPayment(tripId: String, uid: String, amount: Double, actorName: String) {
        tripsCollection.document(tripId).collection("members").document(uid)
            .update(
                mapOf(
                    "pendingPaymentAmount" to 0.0,
                    "pendingPaymentStatus" to "rejected"
                )
            ).await()
        logPaymentEvent(tripId, uid, "rejected", amount, actorName)
    }

    suspend fun revertApprovedPayment(
        tripId: String,
        uid: String,
        newAmountPaid: Double,
        eventAmount: Double,
        actorName: String
    ) {
        tripsCollection.document(tripId).collection("members").document(uid)
            .update(
                mapOf(
                    "amountPaid" to newAmountPaid,
                    "pendingPaymentAmount" to eventAmount,
                    "pendingPaymentStatus" to "pending"
                )
            ).await()
        logPaymentEvent(tripId, uid, "reverted", eventAmount, actorName)
    }

    suspend fun revertRejectedPayment(
        tripId: String,
        uid: String,
        eventAmount: Double,
        actorName: String
    ) {
        tripsCollection.document(tripId).collection("members").document(uid)
            .update(
                mapOf(
                    "pendingPaymentAmount" to eventAmount,
                    "pendingPaymentStatus" to "pending"
                )
            ).await()
        logPaymentEvent(tripId, uid, "reverted", eventAmount, actorName)
    }

    private suspend fun logPaymentEvent(
        tripId: String,
        uid: String,
        type: String,
        amount: Double,
        actorName: String
    ) {
        val data = mapOf(
            "type" to type,
            "amount" to amount,
            "actorName" to actorName,
            "timestamp" to System.currentTimeMillis()
        )
        tripsCollection.document(tripId)
            .collection("members").document(uid)
            .collection("paymentHistory")
            .add(data).await()
    }

    fun getPaymentHistory(tripId: String, uid: String): kotlinx.coroutines.flow.Flow<List<PaymentEvent>> =
        callbackFlow {
            val listener = tripsCollection.document(tripId)
                .collection("members").document(uid)
                .collection("paymentHistory")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        Log.e("TripRepository", "getPaymentHistory failed", error)
                        return@addSnapshotListener
                    }
                    val events = snap?.documents?.map { doc ->
                        PaymentEvent(
                            id = doc.id,
                            type = doc.getString("type") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            actorName = doc.getString("actorName") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    } ?: emptyList()
                    trySend(events)
                }
            awaitClose { listener.remove() }
        }

    // ─── Supplies ─────────────────────────────────────────────────────────────

    fun getSupplyItems(tripId: String): Flow<List<SupplyItem>> = callbackFlow {
        val listener = tripsCollection.document(tripId)
            .collection("supplies")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("TripRepository", "getSupplyItems listener failed", error)
                    return@addSnapshotListener
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
                }?.sortedWith(compareBy({ it.category }, { it.sortOrder })) ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getMaxSortOrder(tripId: String, category: String): Int {
        val snap = tripsCollection.document(tripId)
            .collection("supplies")
            .whereEqualTo("category", category)
            .get()
            .await()
        return snap.documents.maxOfOrNull { it.getLong("sortOrder") ?: 0L }?.toInt() ?: -1
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

    // ─── Trip ─────────────────────────────────────────────────────────────────

    suspend fun renameTripName(tripId: String, name: String) {
        tripsCollection.document(tripId).update("name", name).await()
    }

    // ─── House Details ────────────────────────────────────────────────────────

    suspend fun saveHouseDetails(
        tripId: String,
        url: String,
        address: String,
        nights: Int,
        cost: Double,
        checkInMillis: Long,
        checkOutMillis: Long,
        currentURL: String?,
        currentThumbnailURL: String?
    ) {
        val urlChanged = url != (currentURL ?: "")
        val thumbnailMissing = currentThumbnailURL.isNullOrBlank()
        val thumbnailURL: String? = when {
            // URL explicitly cleared → remove thumbnail
            url.isBlank() -> null
            // URL changed, or a URL exists but thumbnail was previously lost → (re-)fetch
            // Falls back to currentThumbnailURL so a failed fetch never nukes a good stored image
            urlChanged || thumbnailMissing -> withContext(Dispatchers.IO) { fetchAndUploadThumbnail(tripId, url) }
                ?: currentThumbnailURL
            // URL unchanged and thumbnail already stored → preserve as-is
            else -> currentThumbnailURL
        }

        tripsCollection.document(tripId).update(
            mapOf(
                "houseURL" to url,
                "address" to address,
                "totalNights" to nights,
                "totalCost" to cost,
                "thumbnailURL" to thumbnailURL,
                "checkInMillis" to checkInMillis,
                "checkOutMillis" to checkOutMillis
            )
        ).await()
    }

    private suspend fun fetchAndUploadThumbnail(tripId: String, urlString: String): String? {
        return try {
            Log.d("TripRepository", "fetchAndUploadThumbnail: fetching HTML from $urlString")
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible)")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val responseCode = conn.responseCode
            Log.d("TripRepository", "fetchAndUploadThumbnail: HTTP $responseCode")
            val html = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val imageUrlString = extractOGImageURL(html)
            if (imageUrlString == null) {
                Log.w("TripRepository", "fetchAndUploadThumbnail: no og:image tag found in HTML (${html.length} chars)")
                return null
            }
            Log.d("TripRepository", "fetchAndUploadThumbnail: og:image = $imageUrlString")

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
            Log.d("TripRepository", "fetchAndUploadThumbnail: downloaded ${bytes.size} bytes from $resolved")

            // Upload to Firebase Storage
            val ref = storage.reference.child("thumbnails/$tripId.jpg")
            ref.putBytes(bytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d("TripRepository", "fetchAndUploadThumbnail: uploaded → $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e("TripRepository", "fetchAndUploadThumbnail failed: ${e::class.simpleName} — ${e.message}", e)
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

    fun getPendingInviteTrips(email: String): Flow<List<Trip>> = callbackFlow {
        val listener = tripsCollection
            .whereArrayContains("pendingInviteEmails", email.trim().lowercase())
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("TripRepository", "getPendingInviteTrips failed for $email: ${error.message}", error)
                }
                val trips = snap?.documents?.map { doc ->
                    Trip(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        ownerId = doc.getString("ownerId") ?: "",
                        houseURL = doc.getString("houseURL") ?: "",
                        thumbnailURL = doc.getString("thumbnailURL"),
                        address = doc.getString("address") ?: "",
                        totalNights = (doc.getLong("totalNights") ?: 0L).toInt(),
                        totalCost = doc.getDouble("totalCost") ?: 0.0,
                        checkInMillis = doc.getLong("checkInMillis") ?: 0L,
                        checkOutMillis = doc.getLong("checkOutMillis") ?: 0L,
                        memberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        pendingInviteEmails = (doc.get("pendingInviteEmails") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        inviteCode = doc.getString("inviteCode"),
                        inviteCodeEnabled = doc.getBoolean("inviteCodeEnabled") ?: true
                    )
                } ?: emptyList()
                trySend(trips)
            }
        awaitClose { listener.remove() }
    }

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

    // ─── Invite Codes ──────────────────────────────────────────────────────────

    suspend fun findTripByInviteCode(code: String): Trip? {
        val normalized = normalizeInviteCode(code)
        val snap = tripsCollection
            .whereEqualTo("inviteCode", normalized)
            .whereEqualTo("inviteCodeEnabled", true)
            .get()
            .await()
        val doc = snap.documents.firstOrNull() ?: return null
        return Trip(
            id = doc.id,
            name = doc.getString("name") ?: "",
            ownerId = doc.getString("ownerId") ?: "",
            houseURL = doc.getString("houseURL") ?: "",
            thumbnailURL = doc.getString("thumbnailURL"),
            address = doc.getString("address") ?: "",
            totalNights = (doc.getLong("totalNights") ?: 0L).toInt(),
            totalCost = doc.getDouble("totalCost") ?: 0.0,
            checkInMillis = doc.getLong("checkInMillis") ?: 0L,
            checkOutMillis = doc.getLong("checkOutMillis") ?: 0L,
            memberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            pendingInviteEmails = (doc.get("pendingInviteEmails") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            inviteCode = doc.getString("inviteCode"),
            inviteCodeEnabled = doc.getBoolean("inviteCodeEnabled") ?: true
        )
    }

    suspend fun joinTripByCode(tripId: String, uid: String, displayName: String, email: String, avatarSeed: Long) {
        val tripRef = tripsCollection.document(tripId)
        val tripDoc = tripRef.get().await()
        val currentMembers = (tripDoc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        if (uid in currentMembers) throw IllegalStateException("Already a member of this trip")

        db.runBatch { batch ->
            batch.update(tripRef, "memberIds", currentMembers + uid)
            batch.set(
                tripRef.collection("members").document(uid),
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

    suspend fun setInviteCodeEnabled(tripId: String, enabled: Boolean) {
        tripsCollection.document(tripId).update("inviteCodeEnabled", enabled).await()
    }

    suspend fun regenerateInviteCode(tripId: String): String {
        val newCode = generateInviteCode()
        tripsCollection.document(tripId).update("inviteCode", newCode).await()
        return newCode
    }

    // ─── Rides ────────────────────────────────────────────────────────────────

    fun getRides(tripId: String): Flow<List<Ride>> = callbackFlow {
        val listener = tripsCollection.document(tripId)
            .collection("rides")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("TripRepository", "getRides listener failed", error)
                }
                val rides = snap?.documents?.map { doc ->
                    Ride(
                        id = doc.id,
                        driverUid = doc.getString("driverUid") ?: "",
                        driverName = doc.getString("driverName") ?: "",
                        vehicleEmoji = doc.getString("vehicleEmoji") ?: "🚗",
                        vehicleLabel = doc.getString("vehicleLabel") ?: "",
                        departureLocation = doc.getString("departureLocation") ?: "",
                        totalSeats = (doc.getLong("totalSeats") ?: 4L).toInt(),
                        departureTime = doc.getLong("departureTime") ?: 0L,
                        returnTime = doc.getLong("returnTime") ?: 0L,
                        notes = doc.getString("notes") ?: "",
                        passengerUids = (doc.get("passengerUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        passengerNames = (doc.get("passengerNames") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                } ?: emptyList()
                trySend(rides)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addRide(tripId: String, ride: Ride) {
        val data = mapOf(
            "driverUid" to ride.driverUid,
            "driverName" to ride.driverName,
            "vehicleEmoji" to ride.vehicleEmoji,
            "vehicleLabel" to ride.vehicleLabel,
            "departureLocation" to ride.departureLocation,
            "totalSeats" to ride.totalSeats,
            "departureTime" to ride.departureTime,
            "returnTime" to ride.returnTime,
            "notes" to ride.notes,
            "passengerUids" to emptyList<String>(),
            "passengerNames" to emptyList<String>()
        )
        tripsCollection.document(tripId).collection("rides").add(data).await()
    }

    suspend fun claimSeat(tripId: String, rideId: String, uid: String, displayName: String) {
        tripsCollection.document(tripId).collection("rides").document(rideId)
            .update(
                "passengerUids", FieldValue.arrayUnion(uid),
                "passengerNames", FieldValue.arrayUnion(displayName)
            ).await()
    }

    suspend fun unclaimSeat(tripId: String, rideId: String, uid: String, displayName: String) {
        tripsCollection.document(tripId).collection("rides").document(rideId)
            .update(
                "passengerUids", FieldValue.arrayRemove(uid),
                "passengerNames", FieldValue.arrayRemove(displayName)
            ).await()
    }

    suspend fun deleteRide(tripId: String, rideId: String) {
        tripsCollection.document(tripId).collection("rides").document(rideId).delete().await()
    }

    suspend fun updateRide(tripId: String, rideId: String, fields: Map<String, Any>) {
        tripsCollection.document(tripId).collection("rides").document(rideId).update(fields).await()
    }

    // ─── Ride Requests ────────────────────────────────────────────────────────

    fun getRideRequests(tripId: String): Flow<List<RideRequest>> = callbackFlow {
        val listener = tripsCollection.document(tripId)
            .collection("rideRequests")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("TripRepository", "getRideRequests listener failed", error)
                }
                val requests = snap?.documents?.map { doc ->
                    RideRequest(
                        uid = doc.getString("uid") ?: doc.id,
                        displayName = doc.getString("displayName") ?: "",
                        notes = doc.getString("notes") ?: ""
                    )
                } ?: emptyList()
                trySend(requests)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addRideRequest(tripId: String, request: RideRequest) {
        val data = mapOf(
            "uid" to request.uid,
            "displayName" to request.displayName,
            "notes" to request.notes
        )
        tripsCollection.document(tripId).collection("rideRequests").document(request.uid).set(data).await()
    }

    suspend fun removeRideRequest(tripId: String, uid: String) {
        tripsCollection.document(tripId).collection("rideRequests").document(uid).delete().await()
    }

    // ─── Expenses ──────────────────────────────────────────────────────────────

    fun getExpenses(tripId: String): Flow<List<SharedExpense>> = callbackFlow {
        val listener = tripsCollection.document(tripId)
            .collection("expenses")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("TripRepository", "getExpenses listener failed", error)
                }
                val expenses = snap?.documents?.map { doc ->
                    SharedExpense(
                        id = doc.id,
                        description = doc.getString("description") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        category = doc.getString("category") ?: "misc",
                        splitMethod = doc.getString("splitMethod") ?: "even",
                        submittedByUid = doc.getString("submittedByUid") ?: "",
                        submittedByName = doc.getString("submittedByName") ?: "",
                        approved = doc.getBoolean("approved") ?: false,
                        linkedSupplyId = doc.getString("linkedSupplyId"),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(expenses)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addExpense(tripId: String, expense: SharedExpense) {
        val data = mapOf(
            "description" to expense.description,
            "amount" to expense.amount,
            "category" to expense.category,
            "splitMethod" to expense.splitMethod,
            "submittedByUid" to expense.submittedByUid,
            "submittedByName" to expense.submittedByName,
            "approved" to expense.approved,
            "linkedSupplyId" to expense.linkedSupplyId,
            "createdAt" to expense.createdAt
        )
        tripsCollection.document(tripId).collection("expenses").add(data).await()
    }

    suspend fun approveExpense(tripId: String, expenseId: String) {
        tripsCollection.document(tripId).collection("expenses").document(expenseId)
            .update("approved", true).await()
    }

    suspend fun deleteExpense(tripId: String, expenseId: String) {
        tripsCollection.document(tripId).collection("expenses").document(expenseId).delete().await()
    }
}
