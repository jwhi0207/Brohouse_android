package com.bennybokki.frientrip

import com.bennybokki.frientrip.data.TripMember
import com.bennybokki.frientrip.data.TripRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the guest member feature.
 *
 * A guest is a placeholder member added by the trip owner for someone who doesn't
 * have the app. Guests are included in cost calculations but have no app access.
 *
 * Covers:
 * 1. Guest badge and action-button visibility logic (pure).
 * 2. Guest members included in cost calculations (pure).
 * 3. TripRepository.addGuestMember — writes correct Firestore fields.
 * 4. TripRepository.removeGuestMember — deletes the member document.
 */
class GuestMemberTest {

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun guest(
        uid: String = "guest_abc",
        name: String = "John (guest)"
    ) = TripMember(uid = uid, displayName = name, email = "", isGuest = true)

    private fun regularMember(
        uid: String = "uid1",
        name: String = "Alice",
        email: String = "alice@example.com"
    ) = TripMember(uid = uid, displayName = name, email = email)

    private fun deactivatedMember(uid: String = "uid2", name: String = "Bob") =
        TripMember(uid = uid, displayName = name, email = "bob@example.com", status = "deactivated")

    /** Mirrors ManageGroupMemberRow action-button selection. */
    private enum class MemberAction { REMOVE_GUEST, DEACTIVATE, REACTIVATE, NONE }

    private fun actionFor(member: TripMember, isProtected: Boolean): MemberAction = when {
        isProtected -> MemberAction.NONE
        member.isGuest -> MemberAction.REMOVE_GUEST
        member.isDeactivated -> MemberAction.REACTIVATE
        else -> MemberAction.DEACTIVATE
    }

    /** Mirrors TripViewModel.memberCosts: only non-deactivated members are split. */
    private fun isIncludedInCostCalc(member: TripMember): Boolean = !member.isDeactivated

    // ─── Guest badge visibility ────────────────────────────────────────────────

    @Test
    fun `guest member has isGuest true`() {
        assertTrue(guest().isGuest)
    }

    @Test
    fun `regular member has isGuest false by default`() {
        assertFalse(regularMember().isGuest)
    }

    @Test
    fun `guest badge shown for guest, not for regular or deactivated member`() {
        assertTrue(guest().isGuest)
        assertFalse(regularMember().isGuest)
        assertFalse(deactivatedMember().isGuest)
    }

    // ─── Action button logic ──────────────────────────────────────────────────

    @Test
    fun `guest shows Remove button`() {
        assertEquals(MemberAction.REMOVE_GUEST, actionFor(guest(), isProtected = false))
    }

    @Test
    fun `regular active member shows Deactivate button`() {
        assertEquals(MemberAction.DEACTIVATE, actionFor(regularMember(), isProtected = false))
    }

    @Test
    fun `deactivated member shows Reactivate button`() {
        assertEquals(MemberAction.REACTIVATE, actionFor(deactivatedMember(), isProtected = false))
    }

    @Test
    fun `protected member shows no action button`() {
        assertEquals(MemberAction.NONE, actionFor(regularMember(), isProtected = true))
        assertEquals(MemberAction.NONE, actionFor(guest(), isProtected = true))
    }

    @Test
    fun `guest action is Remove even if somehow marked deactivated`() {
        // isGuest check comes first — guests never show deactivate or reactivate
        val deactivatedGuest = guest().copy(status = "deactivated")
        assertEquals(MemberAction.REMOVE_GUEST, actionFor(deactivatedGuest, isProtected = false))
    }

    // ─── Guest in cost calculations ───────────────────────────────────────────

    @Test
    fun `active guest is included in cost calculations`() {
        assertTrue(isIncludedInCostCalc(guest()))
    }

    @Test
    fun `active regular member is included in cost calculations`() {
        assertTrue(isIncludedInCostCalc(regularMember()))
    }

    @Test
    fun `deactivated member is excluded from cost calculations`() {
        assertFalse(isIncludedInCostCalc(deactivatedMember()))
    }

    @Test
    fun `cost calculation active count includes guests`() {
        val members = listOf(
            regularMember("u1"),
            guest("guest_1"),
            guest("guest_2"),
            deactivatedMember("u2")
        )
        val active = members.filter { isIncludedInCostCalc(it) }
        assertEquals(3, active.size) // 1 regular + 2 guests
        assertTrue(active.all { !it.isDeactivated })
    }

    // ─── Guest data defaults ──────────────────────────────────────────────────

    @Test
    fun `guest has empty email`() {
        assertEquals("", guest().email)
    }

    @Test
    fun `guest starts with active status`() {
        assertFalse(guest().isDeactivated)
    }

    @Test
    fun `guest starts with zero nights and zero amount paid`() {
        val g = guest()
        assertEquals(0, g.nightsStayed)
        assertEquals(0.0, g.amountPaid, 0.001)
    }

    // ─── Repository: addGuestMember ────────────────────────────────────────────

    @Test
    fun `addGuestMember writes isGuest true to Firestore`() = runTest {
        val capturedData = mutableMapOf<String, Any?>()
        val db = buildAddGuestMock(tripId = "trip1", onSet = { capturedData += it })

        TripRepository(db = db, storage = mockk(relaxed = true))
            .addGuestMember("trip1", "Dave (guest)")

        assertEquals(true, capturedData["isGuest"])
    }

    @Test
    fun `addGuestMember trims display name`() = runTest {
        val capturedData = mutableMapOf<String, Any?>()
        val db = buildAddGuestMock(tripId = "trip1", onSet = { capturedData += it })

        TripRepository(db = db, storage = mockk(relaxed = true))
            .addGuestMember("trip1", "  Dave  ")

        assertEquals("Dave", capturedData["displayName"])
    }

    @Test
    fun `addGuestMember sets empty email`() = runTest {
        val capturedData = mutableMapOf<String, Any?>()
        val db = buildAddGuestMock(tripId = "trip1", onSet = { capturedData += it })

        TripRepository(db = db, storage = mockk(relaxed = true))
            .addGuestMember("trip1", "Guest")

        assertEquals("", capturedData["email"])
    }

    @Test
    fun `addGuestMember sets status to active`() = runTest {
        val capturedData = mutableMapOf<String, Any?>()
        val db = buildAddGuestMock(tripId = "trip1", onSet = { capturedData += it })

        TripRepository(db = db, storage = mockk(relaxed = true))
            .addGuestMember("trip1", "Guest")

        assertEquals("active", capturedData["status"])
    }

    @Test
    fun `addGuestMember sets uid to the generated document id`() = runTest {
        val capturedData = mutableMapOf<String, Any?>()
        val db = buildAddGuestMock(
            tripId = "trip1",
            generatedDocId = "guest_generated_id",
            onSet = { capturedData += it }
        )

        val returnedId = TripRepository(db = db, storage = mockk(relaxed = true))
            .addGuestMember("trip1", "Guest")

        assertEquals("guest_generated_id", returnedId)
        assertEquals("guest_generated_id", capturedData["uid"])
    }

    @Test
    fun `addGuestMember does not touch memberIds on the trip document`() = runTest {
        var tripDocUpdated = false
        val db = buildAddGuestMock(
            tripId = "trip1",
            onSet = {},
            onTripUpdate = { tripDocUpdated = true }
        )

        TripRepository(db = db, storage = mockk(relaxed = true))
            .addGuestMember("trip1", "Guest")

        assertFalse("addGuestMember should not modify the trip document", tripDocUpdated)
    }

    // ─── Repository: removeGuestMember ────────────────────────────────────────

    @Test
    fun `removeGuestMember deletes the member document`() = runTest {
        var deletedDocId: String? = null
        val db = buildRemoveGuestMock(
            tripId = "trip2",
            guestId = "guest_abc",
            onDelete = { deletedDocId = it }
        )

        TripRepository(db = db, storage = mockk(relaxed = true))
            .removeGuestMember("trip2", "guest_abc")

        assertEquals("guest_abc", deletedDocId)
    }

    @Test
    fun `removeGuestMember does not touch the trip document`() = runTest {
        var tripDocTouched = false
        val db = buildRemoveGuestMock(
            tripId = "trip2",
            guestId = "guest_abc",
            onDelete = {},
            onTripUpdate = { tripDocTouched = true }
        )

        TripRepository(db = db, storage = mockk(relaxed = true))
            .removeGuestMember("trip2", "guest_abc")

        assertFalse("removeGuestMember should not modify the trip document", tripDocTouched)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun buildAddGuestMock(
        tripId: String,
        generatedDocId: String = "auto_generated_id",
        onSet: (Map<String, Any?>) -> Unit,
        onTripUpdate: () -> Unit = {}
    ): FirebaseFirestore {
        val db = mockk<FirebaseFirestore>()
        val tripsCol = mockk<CollectionReference>()
        val tripRef = mockk<DocumentReference>()
        val membersCol = mockk<CollectionReference>()
        val memberDocRef = mockk<DocumentReference>()

        every { db.collection("trips") } returns tripsCol
        every { tripsCol.document(tripId) } returns tripRef
        every { tripRef.collection("members") } returns membersCol
        every { membersCol.document() } returns memberDocRef
        every { memberDocRef.id } returns generatedDocId
        every { memberDocRef.set(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            onSet(firstArg<Map<String, Any?>>())
            completedVoidTask()
        }

        // Any update on the trip doc itself should trigger the callback
        every { tripRef.update(any<Map<String, Any>>()) } answers {
            onTripUpdate()
            completedVoidTask()
        }

        return db
    }

    private fun buildRemoveGuestMock(
        tripId: String,
        guestId: String,
        onDelete: (String) -> Unit,
        onTripUpdate: () -> Unit = {}
    ): FirebaseFirestore {
        val db = mockk<FirebaseFirestore>()
        val tripsCol = mockk<CollectionReference>()
        val tripRef = mockk<DocumentReference>()
        val membersCol = mockk<CollectionReference>()
        val memberDocRef = mockk<DocumentReference>()

        every { db.collection("trips") } returns tripsCol
        every { tripsCol.document(tripId) } returns tripRef
        every { tripRef.collection("members") } returns membersCol
        every { membersCol.document(guestId) } returns memberDocRef
        every { memberDocRef.delete() } answers {
            onDelete(guestId)
            completedVoidTask()
        }

        every { tripRef.update(any<Map<String, Any>>()) } answers {
            onTripUpdate()
            completedVoidTask()
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
}
