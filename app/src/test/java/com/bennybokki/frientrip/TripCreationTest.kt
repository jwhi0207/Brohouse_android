package com.bennybokki.frientrip

import com.bennybokki.frientrip.data.TripRepository
import com.bennybokki.frientrip.data.UserRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.storage.FirebaseStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCreationTest {

    // ─── isAdminEmail / roleForEmail (pure) ──────────────────────────────────

    @Test
    fun `known admin email returns true`() {
        assertTrue(UserRepository.isAdminEmail("jwhi0207@gmail.com"))
        assertTrue(UserRepository.isAdminEmail("benjamincroberts@gmail.com"))
    }

    @Test
    fun `admin email check trims and lowercases`() {
        assertTrue(UserRepository.isAdminEmail("  JWHI0207@GMAIL.COM  "))
        assertTrue(UserRepository.isAdminEmail("BenjaminCroberts@Gmail.Com"))
    }

    @Test
    fun `non-admin email returns false`() {
        assertFalse(UserRepository.isAdminEmail("thiccbokki@gmail.com"))
        assertFalse(UserRepository.isAdminEmail(""))
    }

    @Test
    fun `roleForEmail returns admin for admin email`() {
        assertEquals("admin", UserRepository.roleForEmail("jwhi0207@gmail.com"))
    }

    @Test
    fun `roleForEmail returns user for regular email`() {
        assertEquals("user", UserRepository.roleForEmail("regular@example.com"))
    }

    // ─── createTrip data structure ───────────────────────────────────────────

    @Test
    fun `createTrip writes correct trip document fields`() = runTest {
        val capturedTripData = mutableListOf<Map<String, Any?>>()
        val capturedMemberData = mutableListOf<Map<String, Any?>>()

        val db = buildCreateTripMock(
            generatedTripId = "trip_abc",
            onTripSet = { capturedTripData += it },
            onMemberSet = { capturedMemberData += it }
        )

        val repo = TripRepository(db = db, storage = mockk(relaxed = true))
        val tripId = repo.createTrip(
            name = "Beach Weekend",
            ownerId = "uid_owner",
            ownerDisplayName = "John",
            ownerEmail = "john@example.com",
            ownerAvatarSeed = 12345L
        )

        assertEquals("trip_abc", tripId)

        val trip = capturedTripData.first()
        assertEquals("Beach Weekend", trip["name"])
        assertEquals("uid_owner", trip["ownerId"])
        assertEquals(listOf("uid_owner"), trip["memberIds"])
        assertEquals(emptyList<String>(), trip["deactivatedMemberIds"])
        assertEquals(emptyList<String>(), trip["pendingInviteEmails"])
        assertEquals(0, trip["totalNights"])
        assertEquals(0.0, trip["totalCost"])
        assertEquals("", trip["houseURL"])
        assertEquals("", trip["description"])
        assertEquals("", trip["emoji"])
        assertEquals(0L, trip["checkInMillis"])
        assertEquals(0L, trip["checkOutMillis"])

        val memberDoc = capturedMemberData.first()
        assertEquals("uid_owner", memberDoc["uid"])
        assertEquals("John", memberDoc["displayName"])
        assertEquals("john@example.com", memberDoc["email"])
        assertEquals(12345L, memberDoc["avatarSeed"])
        assertEquals(0, memberDoc["nightsStayed"])
        assertEquals(0.0, memberDoc["amountPaid"])
    }

    @Test
    fun `createTrip owner is only member initially`() = runTest {
        val capturedTripData = mutableListOf<Map<String, Any?>>()

        val db = buildCreateTripMock(
            generatedTripId = "trip_xyz",
            onTripSet = { capturedTripData += it },
            onMemberSet = {}
        )

        TripRepository(db = db, storage = mockk(relaxed = true)).createTrip(
            name = "Ski Trip",
            ownerId = "owner99",
            ownerDisplayName = "Sam",
            ownerEmail = "sam@example.com",
            ownerAvatarSeed = 0L
        )

        val memberIds = capturedTripData.first()["memberIds"] as List<*>
        assertEquals(1, memberIds.size)
        assertEquals("owner99", memberIds.first())

        val pending = capturedTripData.first()["pendingInviteEmails"] as List<*>
        assertTrue(pending.isEmpty())
    }

    @Test
    fun `createTrip with optional fields writes all data`() = runTest {
        val capturedTripData = mutableListOf<Map<String, Any?>>()

        val db = buildCreateTripMock(
            generatedTripId = "trip_full",
            onTripSet = { capturedTripData += it },
            onMemberSet = {}
        )

        val repo = TripRepository(db = db, storage = mockk(relaxed = true))
        repo.createTrip(
            name = "Lake House",
            ownerId = "uid_1",
            ownerDisplayName = "Alice",
            ownerEmail = "alice@example.com",
            ownerAvatarSeed = 999L,
            address = "123 Lake Rd",
            totalCost = 1500.00,
            checkInMillis = 1700000000000L,
            checkOutMillis = 1700300000000L,
            description = "Annual friends trip",
            emoji = "\uD83C\uDFD6\uFE0F",
            pendingInviteEmails = listOf("bob@example.com", "carol@example.com")
        )

        val trip = capturedTripData.first()
        assertEquals("Lake House", trip["name"])
        assertEquals("123 Lake Rd", trip["address"])
        assertEquals(1500.00, trip["totalCost"])
        assertEquals(1700000000000L, trip["checkInMillis"])
        assertEquals(1700300000000L, trip["checkOutMillis"])
        assertEquals("Annual friends trip", trip["description"])
        assertEquals("\uD83C\uDFD6\uFE0F", trip["emoji"])
        assertEquals(listOf("bob@example.com", "carol@example.com"), trip["pendingInviteEmails"])
    }

    @Test
    fun `createTrip with invite emails populates pendingInviteEmails`() = runTest {
        val capturedTripData = mutableListOf<Map<String, Any?>>()

        val db = buildCreateTripMock(
            generatedTripId = "trip_inv",
            onTripSet = { capturedTripData += it },
            onMemberSet = {}
        )

        TripRepository(db = db, storage = mockk(relaxed = true)).createTrip(
            name = "Invite Test",
            ownerId = "uid_2",
            ownerDisplayName = "Dave",
            ownerEmail = "dave@example.com",
            ownerAvatarSeed = 0L,
            pendingInviteEmails = listOf("eve@example.com")
        )

        val pending = capturedTripData.first()["pendingInviteEmails"] as List<*>
        assertEquals(1, pending.size)
        assertEquals("eve@example.com", pending.first())
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private fun buildCreateTripMock(
        generatedTripId: String,
        onTripSet: (Map<String, Any?>) -> Unit,
        onMemberSet: (Map<String, Any?>) -> Unit
    ): FirebaseFirestore {
        val db = mockk<FirebaseFirestore>()
        val tripsCol = mockk<CollectionReference>()
        val tripRef = mockk<DocumentReference>()
        val membersCol = mockk<CollectionReference>()
        val memberRef = mockk<DocumentReference>()
        val batch = mockk<WriteBatch>(relaxed = true)

        every { db.collection("trips") } returns tripsCol
        every { tripsCol.document() } returns tripRef
        every { tripRef.id } returns generatedTripId
        every { tripRef.collection("members") } returns membersCol
        every { membersCol.document(any()) } returns memberRef

        @Suppress("UNCHECKED_CAST")
        every { batch.set(eq(tripRef), any()) } answers {
            onTripSet(secondArg<Map<String, Any?>>())
            batch
        }
        @Suppress("UNCHECKED_CAST")
        every { batch.set(eq(memberRef), any()) } answers {
            onMemberSet(secondArg<Map<String, Any?>>())
            batch
        }

        // Use any() + firstArg because runBatch takes WriteBatch.Function (Java SAM),
        // not a Kotlin function type — slot<(WriteBatch)->Unit> would fail to match.
        every { db.runBatch(any()) } answers {
            firstArg<WriteBatch.Function>().apply(batch)
            completedVoidTask()
        }

        return db
    }

    /**
     * Creates an already-completed Task for Void operations (e.g. runBatch, set, update).
     * Avoids the "parameter is always null" warning from completedTask<Void?>(null).
     */
    private fun completedVoidTask(): Task<Void?> {
        val task = mockk<Task<Void?>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.isCanceled } returns false
        every { task.result } returns null
        every { task.exception } returns null
        return task
    }

    /** Creates an already-completed Task with a non-null [value]. */
    private fun <T : Any> completedTask(value: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isComplete } returns true
        every { task.isSuccessful } returns true
        every { task.isCanceled } returns false
        every { task.result } returns value
        every { task.exception } returns null
        return task
    }
}
