package com.bennybokki.frientrip

import com.bennybokki.frientrip.data.UserRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InviteLogicTest {

    // ─── Email normalisation (pure logic) ───────────────────────────────────

    @Test
    fun `isAdminEmail is case-insensitive`() {
        assert(UserRepository.isAdminEmail("JWHI0207@GMAIL.COM"))
        assert(UserRepository.isAdminEmail("  jwhi0207@gmail.com  "))
        assert(!UserRepository.isAdminEmail("other@gmail.com"))
    }

    // ─── checkAndAcceptPendingInvites ────────────────────────────────────────

    /**
     * Build a minimal Firestore mock that returns [docs] for
     * `whereArrayContains("pendingInviteEmails", normalizedEmail).get()`.
     */
    private fun buildFirestoreMock(
        normalizedEmail: String,
        docs: List<DocumentSnapshot>,
        capturedBatchOps: MutableList<Map<String, Any>> = mutableListOf()
    ): FirebaseFirestore {
        val db = mockk<FirebaseFirestore>()
        val tripsCol = mockk<CollectionReference>()
        val usersCol = mockk<CollectionReference>()
        val query = mockk<Query>()
        val querySnap = mockk<QuerySnapshot>()
        val batch = mockk<WriteBatch>(relaxed = true)

        val tripDocRef = mockk<DocumentReference>()
        val membersTripCol = mockk<CollectionReference>()
        val memberDocRef = mockk<DocumentReference>()

        every { db.collection("trips") } returns tripsCol
        every { db.collection("users") } returns usersCol

        every { tripsCol.document(any()) } returns tripDocRef
        every { tripDocRef.collection("members") } returns membersTripCol
        every { membersTripCol.document(any()) } returns memberDocRef

        every { tripsCol.whereArrayContains("pendingInviteEmails", normalizedEmail) } returns query
        every { query.get() } returns completedTask(querySnap)
        every { querySnap.documents } returns docs

        // Invoke the batch function synchronously when runBatch is called.
        // Use any() + firstArg because runBatch takes WriteBatch.Function (Java SAM),
        // not a Kotlin function type — slot<(WriteBatch)->Unit> would fail to match.
        every { db.runBatch(any()) } answers {
            firstArg<WriteBatch.Function>().apply(batch)
            completedVoidTask()
        }
        every { batch.update(any<DocumentReference>(), any<Map<String, Any>>()) } returns batch
        every { batch.set(any<DocumentReference>(), any<Map<String, Any>>()) } returns batch

        return db
    }

    private fun mockTripDoc(
        tripId: String,
        memberIds: List<String>,
        pendingEmails: List<String>
    ): DocumentSnapshot {
        val doc = mockk<DocumentSnapshot>()
        every { doc.id } returns tripId
        every { doc.get("pendingInviteEmails") } returns pendingEmails
        every { doc.get("memberIds") } returns memberIds
        // DocumentReference for the trip doc
        val tripRef = mockk<DocumentReference>()
        every { tripRef.id } returns tripId
        return doc
    }

    @Test
    fun `uppercase email in invite is matched after normalization`() = runTest {
        // Simulate: trip was invited with "  Thiccbokki@Gmail.Com  " but stored normalized
        val normalizedEmail = "thiccbokki@gmail.com"
        val doc = mockTripDoc("trip1", memberIds = listOf("owner1"), pendingEmails = listOf(normalizedEmail))

        val db = buildFirestoreMock(normalizedEmail, listOf(doc))
        val repo = UserRepository(db)

        // Should not throw; accepts invite for user with mixed-case email
        repo.checkAndAcceptPendingInvites(
            email = "  Thiccbokki@Gmail.Com  ",
            uid = "uid123",
            displayName = "Thicc Bokki",
            avatarSeed = 42L
        )
        // If we got here without exception, normalization worked
    }

    @Test
    fun `user already a member is skipped`() = runTest {
        val normalizedEmail = "user@example.com"
        // uid already in memberIds
        val doc = mockTripDoc("trip1", memberIds = listOf("uid123"), pendingEmails = listOf(normalizedEmail))

        val db = buildFirestoreMock(normalizedEmail, listOf(doc))
        val repo = UserRepository(db)

        repo.checkAndAcceptPendingInvites(
            email = normalizedEmail,
            uid = "uid123",
            displayName = "User",
            avatarSeed = 0L
        )

        // batch.update should never be called since member is already present
        verify(exactly = 0) { db.runBatch(any()) }
    }

    @Test
    fun `no pending trips results in no batch writes`() = runTest {
        val normalizedEmail = "nobody@example.com"
        val db = buildFirestoreMock(normalizedEmail, emptyList())
        val repo = UserRepository(db)

        repo.checkAndAcceptPendingInvites(
            email = normalizedEmail,
            uid = "uid999",
            displayName = "Nobody",
            avatarSeed = 0L
        )

        verify(exactly = 0) { db.runBatch(any()) }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    /**
     * Creates an already-completed Task for Void operations (e.g. runBatch).
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
