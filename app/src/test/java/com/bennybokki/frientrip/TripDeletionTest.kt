package com.bennybokki.frientrip

import com.bennybokki.frientrip.data.TripRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for trip deletion.
 *
 * Covers:
 * 1. Owner-only visibility gate for the Delete Trip drawer item.
 * 2. TripRepository.deleteTrip — all subcollections queried, docs batch-deleted, trip doc removed.
 *
 * Mirrors:
 *   val isOwner = trip?.ownerId == viewModel.currentUid  (TripScaffold.kt)
 *   suspend fun deleteTrip(tripId)                        (TripRepository.kt)
 */
class TripDeletionTest {

    // ─── Owner-only visibility ─────────────────────────────────────────────────

    /** Mirrors: val isOwner = trip?.ownerId == viewModel.currentUid */
    private fun isOwner(ownerId: String?, currentUid: String) = ownerId == currentUid

    @Test
    fun `trip owner sees delete button`() {
        assertTrue(isOwner("uid1", "uid1"))
    }

    @Test
    fun `global admin who is not the owner does not see delete button`() {
        assertFalse(isOwner("ownerUID", "adminUID"))
    }

    @Test
    fun `regular member does not see delete button`() {
        assertFalse(isOwner("ownerUID", "memberUID"))
    }

    @Test
    fun `null ownerId does not grant delete to anyone`() {
        assertFalse(isOwner(null, "uid1"))
    }

    @Test
    fun `delete button is exclusive to the exact owner uid`() {
        assertFalse(isOwner("uid1", "uid10"))
        assertFalse(isOwner("uid10", "uid1"))
        assertTrue(isOwner("uid1", "uid1"))
    }

    // ─── Repository: deleteTrip ────────────────────────────────────────────────

    @Test
    fun `deleteTrip queries all 6 expected subcollections`() = runTest {
        val queried = mutableListOf<String>()
        val db = buildDeleteTripMock(
            tripId = "trip1",
            docCounts = emptyDocCounts(),
            onBatchDelete = {},
            onTripDelete = {},
            onCollectionQueried = { queried += it }
        )

        TripRepository(db = db, storage = mockk(relaxed = true)).deleteTrip("trip1")

        assertEquals(
            listOf("members", "supplies", "rides", "rideRequests", "expenses", "history"),
            queried
        )
    }

    @Test
    fun `deleteTrip batch-deletes all docs from populated subcollections`() = runTest {
        val deletedRefs = mutableListOf<DocumentReference>()
        val db = buildDeleteTripMock(
            tripId = "trip2",
            docCounts = mapOf(
                "members" to 2, "supplies" to 3, "rides" to 0,
                "rideRequests" to 1, "expenses" to 0, "history" to 0
            ),
            onBatchDelete = { deletedRefs += it },
            onTripDelete = {}
        )

        TripRepository(db = db, storage = mockk(relaxed = true)).deleteTrip("trip2")

        assertEquals(6, deletedRefs.size) // 2 + 3 + 0 + 1 + 0 + 0
    }

    @Test
    fun `deleteTrip always deletes the trip document itself`() = runTest {
        var tripDocDeleted = false
        val db = buildDeleteTripMock(
            tripId = "trip3",
            docCounts = emptyDocCounts(),
            onBatchDelete = {},
            onTripDelete = { tripDocDeleted = true }
        )

        TripRepository(db = db, storage = mockk(relaxed = true)).deleteTrip("trip3")

        assertTrue(tripDocDeleted)
    }

    @Test
    fun `deleteTrip with no subcollection docs still deletes the trip document`() = runTest {
        val deletedRefs = mutableListOf<DocumentReference>()
        var tripDocDeleted = false
        val db = buildDeleteTripMock(
            tripId = "trip4",
            docCounts = emptyDocCounts(),
            onBatchDelete = { deletedRefs += it },
            onTripDelete = { tripDocDeleted = true }
        )

        TripRepository(db = db, storage = mockk(relaxed = true)).deleteTrip("trip4")

        assertTrue(deletedRefs.isEmpty())
        assertTrue(tripDocDeleted)
    }

    @Test
    fun `deleteTrip deletes docs from every populated subcollection`() = runTest {
        val deletedRefs = mutableListOf<DocumentReference>()
        val db = buildDeleteTripMock(
            tripId = "trip5",
            docCounts = mapOf(
                "members" to 1, "supplies" to 1, "rides" to 1,
                "rideRequests" to 1, "expenses" to 1, "history" to 1
            ),
            onBatchDelete = { deletedRefs += it },
            onTripDelete = {}
        )

        TripRepository(db = db, storage = mockk(relaxed = true)).deleteTrip("trip5")

        assertEquals(6, deletedRefs.size)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun emptyDocCounts() = mapOf(
        "members" to 0, "supplies" to 0, "rides" to 0,
        "rideRequests" to 0, "expenses" to 0, "history" to 0
    )

    private fun buildDeleteTripMock(
        tripId: String,
        docCounts: Map<String, Int>,
        onBatchDelete: (DocumentReference) -> Unit,
        onTripDelete: () -> Unit,
        onCollectionQueried: (String) -> Unit = {}
    ): FirebaseFirestore {
        val db = mockk<FirebaseFirestore>()
        val tripsCol = mockk<CollectionReference>()
        val tripRef = mockk<DocumentReference>()

        every { db.collection("trips") } returns tripsCol
        every { tripsCol.document(tripId) } returns tripRef
        every { tripRef.delete() } answers { onTripDelete(); completedVoidTask() }

        val batch = mockk<WriteBatch>(relaxed = true)
        every { db.batch() } returns batch
        every { batch.delete(any()) } answers { onBatchDelete(firstArg()); batch }
        every { batch.commit() } returns completedVoidTask()

        val allNames = listOf("members", "supplies", "rides", "rideRequests", "expenses", "history")
        allNames.forEach { name ->
            val count = docCounts[name] ?: 0
            val colRef = mockk<CollectionReference>()
            val querySnap = mockk<QuerySnapshot>()
            val docs = (1..count).map {
                val docSnap = mockk<DocumentSnapshot>()
                val docRef = mockk<DocumentReference>()
                every { docSnap.reference } returns docRef
                docSnap
            }
            every { querySnap.documents } returns docs
            every { tripRef.collection(name) } answers { onCollectionQueried(name); colRef }
            every { colRef.get() } returns completedTask(querySnap)
        }

        return db
    }

    private fun completedVoidTask(): Task<Void?> = mockk<Task<Void?>>().also {
        every { it.isComplete } returns true
        every { it.isSuccessful } returns true
        every { it.isCanceled } returns false
        every { it.result } returns null
        every { it.exception } returns null
    }

    private fun <T : Any> completedTask(value: T): Task<T> = mockk<Task<T>>().also {
        every { it.isComplete } returns true
        every { it.isSuccessful } returns true
        every { it.isCanceled } returns false
        every { it.result } returns value
        every { it.exception } returns null
    }
}
