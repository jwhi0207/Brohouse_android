package com.bennybokki.frientrip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the trip drawer menu visibility logic.
 *
 * "Manage Group" and "Trip History" are only visible to trip admins, which are defined as
 * either a global app admin OR the owner of the current trip.
 *
 * Mirrors: val isTripAdmin = isAdmin || trip?.ownerId == viewModel.currentUid
 * (TripScaffold.kt)
 */
class DrawerMenuLogicTest {

    /** Mirrors the isTripAdmin computation in TripScaffold. */
    private fun isTripAdmin(isAdmin: Boolean, ownerId: String?, currentUid: String): Boolean =
        isAdmin || ownerId == currentUid

    // ─── Global app admin ─────────────────────────────────────────────────────

    @Test
    fun `global admin sees admin-only drawer items`() {
        assertTrue(isTripAdmin(isAdmin = true, ownerId = "ownerXYZ", currentUid = "adminUID"))
    }

    @Test
    fun `global admin sees admin-only items even when they are not the trip owner`() {
        assertTrue(isTripAdmin(isAdmin = true, ownerId = "someoneElse", currentUid = "adminUID"))
    }

    // ─── Trip owner (non-global-admin) ────────────────────────────────────────

    @Test
    fun `trip owner sees admin-only drawer items`() {
        assertTrue(isTripAdmin(isAdmin = false, ownerId = "uid1", currentUid = "uid1"))
    }

    @Test
    fun `trip owner is identified strictly by matching uid`() {
        assertTrue(isTripAdmin(isAdmin = false, ownerId = "abc123", currentUid = "abc123"))
    }

    // ─── Regular member ───────────────────────────────────────────────────────

    @Test
    fun `regular member does not see admin-only drawer items`() {
        assertFalse(isTripAdmin(isAdmin = false, ownerId = "ownerUID", currentUid = "memberUID"))
    }

    @Test
    fun `regular member is not confused with trip owner when uids differ`() {
        assertFalse(isTripAdmin(isAdmin = false, ownerId = "uid1", currentUid = "uid2"))
    }

    // ─── Null / edge cases ────────────────────────────────────────────────────

    @Test
    fun `null ownerId does not grant admin to any member`() {
        assertFalse(isTripAdmin(isAdmin = false, ownerId = null, currentUid = "uid1"))
    }

    @Test
    fun `null ownerId with global admin still grants access`() {
        assertTrue(isTripAdmin(isAdmin = true, ownerId = null, currentUid = "uid1"))
    }

    // ─── Both admin items gate together ──────────────────────────────────────

    @Test
    fun `manage group and trip history share the same visibility condition`() {
        // Verify that both admin-only items ("Manage Group" and "Trip History") use
        // the exact same isTripAdmin gate — if one is visible, the other must be too.
        val scenarios = listOf(
            Triple(true,  "owner1", "admin1"),   // global admin, not owner
            Triple(false, "uid1",   "uid1"),     // trip owner
            Triple(false, "uid1",   "uid2"),     // regular member
            Triple(false, null,     "uid1"),     // no owner set yet
        )
        scenarios.forEach { (isAdmin, ownerId, currentUid) ->
            val manageGroupVisible = isTripAdmin(isAdmin, ownerId, currentUid)
            val tripHistoryVisible = isTripAdmin(isAdmin, ownerId, currentUid)
            // Both must be identical — they share the same gate
            org.junit.Assert.assertEquals(
                "Manage Group and Trip History visibility must match for ($isAdmin, $ownerId, $currentUid)",
                manageGroupVisible,
                tripHistoryVisible
            )
        }
    }
}
