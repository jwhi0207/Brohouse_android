package com.bennybokki.frientrip

import com.bennybokki.frientrip.data.TripRepository
import com.bennybokki.frientrip.data.UserRepository
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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

        val repo = TripRepository(db = db)
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
        assertEquals(emptyList<String>(), trip["pendingInviteEmails"])
        assertEquals(0, trip["totalNights"])
        assertEquals(0.0, trip["totalCost"])
        assertEquals("", trip["houseURL"])

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

        TripRepository(db = db).createTrip(
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

        val batchFnSlot = slot<(WriteBatch) -> Unit>()
        every { db.runBatch(capture(batchFnSlot)) } answers {
            batchFnSlot.captured(batch)
            Tasks.forResult<Void?>(null)
        }

        return db
    }

    private fun <T> completedTask(result: T): Task<T> = Tasks.forResult(result)
}
