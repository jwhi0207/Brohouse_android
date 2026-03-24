package com.bennybokki.frientrip

import com.bennybokki.frientrip.data.SupplyItem
import com.bennybokki.frientrip.data.TripMember
import com.bennybokki.frientrip.data.UserRepository
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for member deactivation / reactivation logic in ManageGroupScreen.
 *
 * Core rules:
 * - Trip owners and global app admins are protected — no deactivate button shown.
 * - Deactivating removes the member from trip access (modeled as status="deactivated").
 * - Reactivating restores status to "active".
 * - Deactivated members cannot rejoin via invite code.
 */
class MemberDeactivationTest {

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun member(
        uid: String,
        email: String = "$uid@example.com",
        status: String = "active"
    ) = TripMember(uid = uid, displayName = uid, email = email, status = status)

    /**
     * Mirrors the isProtected check in ManageGroupMemberRow:
     * owner OR global admin → no action button.
     */
    private fun isProtected(member: TripMember, ownerId: String): Boolean =
        member.uid == ownerId || UserRepository.isAdminEmail(member.email)

    /**
     * Mirrors the joinTripByCode deactivation guard:
     * a member with status="deactivated" must not be allowed to rejoin via invite code.
     */
    private fun canRejoinViaCode(memberDoc: TripMember?, isInMemberIds: Boolean): Boolean {
        if (isInMemberIds) return false // already a member
        if (memberDoc != null && memberDoc.isDeactivated) return false // was deactivated
        return true
    }

    // ─── TripMember status helpers ────────────────────────────────────────────

    @Test
    fun `new member has active status by default`() {
        val m = TripMember(uid = "u1", displayName = "Alice", email = "alice@example.com")
        assertEquals("active", m.status)
        assertFalse(m.isDeactivated)
    }

    @Test
    fun `member with status deactivated reports isDeactivated true`() {
        val m = member("u1", status = "deactivated")
        assertTrue(m.isDeactivated)
    }

    @Test
    fun `member with status active reports isDeactivated false`() {
        val m = member("u1", status = "active")
        assertFalse(m.isDeactivated)
    }

    @Test
    fun `deactivation modeled as status change`() {
        val active = member("u1")
        val deactivated = active.copy(status = "deactivated")
        assertTrue(deactivated.isDeactivated)
        assertFalse(active.isDeactivated)
    }

    @Test
    fun `reactivation modeled as status change back to active`() {
        val deactivated = member("u1", status = "deactivated")
        val reactivated = deactivated.copy(status = "active")
        assertFalse(reactivated.isDeactivated)
    }

    // ─── Protected member gate ────────────────────────────────────────────────

    @Test
    fun `trip owner is protected — no deactivate button`() {
        val owner = member("owner1")
        assertTrue(isProtected(owner, ownerId = "owner1"))
    }

    @Test
    fun `global admin is protected — no deactivate button`() {
        val admin = member("adminUid", email = "jwhi0207@gmail.com")
        assertTrue(isProtected(admin, ownerId = "someoneElse"))
    }

    @Test
    fun `regular member is not protected — deactivate button shown`() {
        val regular = member("uid2", email = "regular@example.com")
        assertFalse(isProtected(regular, ownerId = "uid1"))
    }

    @Test
    fun `owner who is also a global admin is still protected`() {
        val ownerAdmin = member("uid1", email = "jwhi0207@gmail.com")
        assertTrue(isProtected(ownerAdmin, ownerId = "uid1"))
    }

    @Test
    fun `member whose uid matches ownerId but different email is still protected`() {
        val owner = member("ownerUid", email = "regular@example.com")
        assertTrue(isProtected(owner, ownerId = "ownerUid"))
    }

    // ─── Rejoin via invite code guard ─────────────────────────────────────────

    @Test
    fun `active member already in memberIds cannot join again`() {
        val m = member("u1")
        assertFalse(canRejoinViaCode(m, isInMemberIds = true))
    }

    @Test
    fun `deactivated member cannot rejoin via invite code`() {
        val deactivated = member("u1", status = "deactivated")
        assertFalse(canRejoinViaCode(deactivated, isInMemberIds = false))
    }

    @Test
    fun `brand new user with no existing doc can join via invite code`() {
        assertTrue(canRejoinViaCode(memberDoc = null, isInMemberIds = false))
    }

    @Test
    fun `active member not yet in memberIds can join via invite code`() {
        // Edge case: member doc exists but uid not in memberIds (shouldn't normally happen,
        // but guard should allow them through)
        val active = member("u1", status = "active")
        assertTrue(canRejoinViaCode(active, isInMemberIds = false))
    }

    // ─── UI filter — deactivated members hidden from display lists ────────────

    @Test
    fun `deactivated members are excluded from active member filter`() {
        val members = listOf(
            member("alice", status = "active"),
            member("bob", status = "deactivated"),
            member("charlie", status = "active")
        )
        val activeMembers = members.filter { !it.isDeactivated }
        assertEquals(2, activeMembers.size)
        assertFalse(activeMembers.any { it.uid == "bob" })
    }

    @Test
    fun `active member count excludes deactivated members`() {
        val members = listOf(
            member("a", status = "active"),
            member("b", status = "deactivated"),
            member("c", status = "deactivated"),
            member("d", status = "active")
        )
        val activeCount = members.count { !it.isDeactivated }
        assertEquals(2, activeCount)
    }

    // ─── Supply claim cleanup on deactivation ─────────────────────────────────

    @Test
    fun `deactivating sole claimant clears supply claim entirely`() {
        val item = SupplyItem(
            id = "s1",
            name = "Towels",
            claimedByUids = listOf("uid1"),
            claimedByName = "Alice",
            quantity = "Alice=2"
        )
        val updated = item.removeClaim("uid1", "Alice")
        assertTrue(updated.claimedByUids.isEmpty())
        assertNull(updated.claimedByName)
        assertEquals("", updated.quantity)
    }

    @Test
    fun `deactivating one of multiple claimants leaves other claims intact`() {
        val item = SupplyItem(
            id = "s1",
            name = "Towels",
            claimedByUids = listOf("uid1", "uid2"),
            claimedByName = "Alice, Bob",
            quantity = "Alice=2|Bob=3"
        )
        val updated = item.removeClaim("uid1", "Alice")
        assertFalse(updated.claimedByUids.contains("uid1"))
        assertTrue(updated.claimedByUids.contains("uid2"))
        assertFalse(updated.claimedByName.orEmpty().contains("Alice"))
        assertTrue(updated.claimedByName.orEmpty().contains("Bob"))
    }

    @Test
    fun `removeClaim on item not claimed by user leaves item unchanged`() {
        val item = SupplyItem(
            id = "s1",
            name = "Towels",
            claimedByUids = listOf("uid2"),
            claimedByName = "Bob",
            quantity = "Bob=3"
        )
        val updated = item.removeClaim("uid1", "Alice")
        assertEquals(listOf("uid2"), updated.claimedByUids)
        assertEquals("Bob", updated.claimedByName)
    }

    // ─── Passenger removal — parallel array sync ──────────────────────────────

    @Test
    fun `removing passenger keeps parallel arrays in sync`() {
        val passengerUids = listOf("uid1", "uid2", "uid3")
        val passengerNames = listOf("Alice", "Bob", "Charlie")
        val uidToRemove = "uid2"

        val idx = passengerUids.indexOf(uidToRemove)
        val updatedUids = passengerUids.filter { it != uidToRemove }
        val updatedNames = passengerNames.filterIndexed { i, _ -> i != idx }

        assertEquals(listOf("uid1", "uid3"), updatedUids)
        assertEquals(listOf("Alice", "Charlie"), updatedNames)
    }

    @Test
    fun `removing first passenger keeps remaining in correct order`() {
        val passengerUids = listOf("uid1", "uid2")
        val passengerNames = listOf("Alice", "Bob")
        val uidToRemove = "uid1"

        val idx = passengerUids.indexOf(uidToRemove)
        val updatedUids = passengerUids.filter { it != uidToRemove }
        val updatedNames = passengerNames.filterIndexed { i, _ -> i != idx }

        assertEquals(listOf("uid2"), updatedUids)
        assertEquals(listOf("Bob"), updatedNames)
    }

    @Test
    fun `removing last passenger yields empty parallel arrays`() {
        val passengerUids = listOf("uid1")
        val passengerNames = listOf("Alice")
        val uidToRemove = "uid1"

        val idx = passengerUids.indexOf(uidToRemove)
        val updatedUids = passengerUids.filter { it != uidToRemove }
        val updatedNames = passengerNames.filterIndexed { i, _ -> i != idx }

        assertTrue(updatedUids.isEmpty())
        assertTrue(updatedNames.isEmpty())
    }

    // ─── Sort order ───────────────────────────────────────────────────────────

    @Test
    fun `active members sort before deactivated members`() {
        val members = listOf(
            member("charlie", status = "deactivated"),
            member("alice", status = "active"),
            member("bob", status = "active")
        )
        val sorted = members.sortedWith(compareBy({ it.isDeactivated }, { it.displayName }))
        assertFalse(sorted[0].isDeactivated) // alice
        assertFalse(sorted[1].isDeactivated) // bob
        assertTrue(sorted[2].isDeactivated)  // charlie
    }

    @Test
    fun `within active members alphabetical order is preserved`() {
        val members = listOf(
            member("zara", status = "active"),
            member("alice", status = "active"),
            member("bob", status = "active")
        )
        val sorted = members.sortedWith(compareBy({ it.isDeactivated }, { it.displayName }))
        assertEquals("alice", sorted[0].displayName)
        assertEquals("bob", sorted[1].displayName)
        assertEquals("zara", sorted[2].displayName)
    }

    @Test
    fun `within deactivated members alphabetical order is preserved`() {
        val members = listOf(
            member("zara", status = "deactivated"),
            member("alice", status = "deactivated")
        )
        val sorted = members.sortedWith(compareBy({ it.isDeactivated }, { it.displayName }))
        assertEquals("alice", sorted[0].displayName)
        assertEquals("zara", sorted[1].displayName)
    }
}
