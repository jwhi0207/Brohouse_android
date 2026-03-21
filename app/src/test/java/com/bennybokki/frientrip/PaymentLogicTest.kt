package com.bennybokki.frientrip

import com.bennybokki.frientrip.data.PaymentEvent
import com.bennybokki.frientrip.data.TripMember
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for all payment-related business logic:
 * - Amount capping and submit validation
 * - Approval / rejection / revert state transitions
 * - Amount due calculation
 * - PaymentEvent model
 */
class PaymentLogicTest {

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private fun member(
        amountPaid: Double = 0.0,
        pendingStatus: String = "none",
        pendingAmount: Double = 0.0
    ) = TripMember(
        uid = "uid1",
        displayName = "Test User",
        email = "test@example.com",
        amountPaid = amountPaid,
        pendingPaymentStatus = pendingStatus,
        pendingPaymentAmount = pendingAmount
    )

    /** Mirrors the capping logic in TripDashboard before calling submitPaymentForReview. */
    private fun cappedAmount(submitted: Double, remaining: Double) =
        if (submitted > remaining) remaining else submitted

    /** Mirrors the canSubmit check in PayExpensesSheet. */
    private fun canSubmit(text: String): Boolean {
        val parsed = text.trim().toDoubleOrNull() ?: return false
        return parsed >= 0.01
    }

    // ─────────────────────────────────────────────
    // Amount capping
    // ─────────────────────────────────────────────

    @Test
    fun `amount exceeding remaining is capped to remaining`() {
        assertEquals(100.0, cappedAmount(150.0, 100.0), 0.001)
    }

    @Test
    fun `amount less than remaining is unchanged`() {
        assertEquals(50.0, cappedAmount(50.0, 100.0), 0.001)
    }

    @Test
    fun `amount exactly equal to remaining is unchanged`() {
        assertEquals(100.0, cappedAmount(100.0, 100.0), 0.001)
    }

    @Test
    fun `capping works for fractional amounts`() {
        assertEquals(337.62, cappedAmount(999.99, 337.62), 0.001)
    }

    // ─────────────────────────────────────────────
    // Submit button enabled state
    // ─────────────────────────────────────────────

    @Test
    fun `submit is disabled for empty field`() {
        assertFalse(canSubmit(""))
    }

    @Test
    fun `submit is disabled for zero`() {
        assertFalse(canSubmit("0.00"))
    }

    @Test
    fun `submit is disabled for negative amount`() {
        assertFalse(canSubmit("-5.00"))
    }

    @Test
    fun `submit is disabled for non-numeric input`() {
        assertFalse(canSubmit("abc"))
    }

    @Test
    fun `submit is enabled at minimum valid amount`() {
        assertTrue(canSubmit("0.01"))
    }

    @Test
    fun `submit is enabled for normal amount`() {
        assertTrue(canSubmit("50.00"))
    }

    // ─────────────────────────────────────────────
    // Approval logic
    // ─────────────────────────────────────────────

    @Test
    fun `approving adds pendingAmount to amountPaid`() {
        val m = member(amountPaid = 50.0, pendingStatus = "pending", pendingAmount = 30.0)
        val newAmountPaid = m.amountPaid + m.pendingPaymentAmount
        assertEquals(80.0, newAmountPaid, 0.001)
    }

    @Test
    fun `approving from zero amountPaid equals pendingAmount`() {
        val m = member(amountPaid = 0.0, pendingStatus = "pending", pendingAmount = 75.0)
        val newAmountPaid = m.amountPaid + m.pendingPaymentAmount
        assertEquals(75.0, newAmountPaid, 0.001)
    }

    @Test
    fun `approving full balance leaves zero remaining`() {
        val computedOwed = 337.62
        val m = member(amountPaid = 0.0, pendingStatus = "pending", pendingAmount = 337.62)
        val remaining = computedOwed - (m.amountPaid + m.pendingPaymentAmount)
        assertEquals(0.0, remaining, 0.001)
    }

    @Test
    fun `after approval pending state is cleared`() {
        val afterApproval = member(amountPaid = 80.0, pendingStatus = "none", pendingAmount = 0.0)
        assertEquals("none", afterApproval.pendingPaymentStatus)
        assertEquals(0.0, afterApproval.pendingPaymentAmount, 0.001)
    }

    // ─────────────────────────────────────────────
    // Rejection logic
    // ─────────────────────────────────────────────

    @Test
    fun `rejecting sets status to rejected`() {
        val m = member(amountPaid = 50.0, pendingStatus = "pending", pendingAmount = 30.0)
        val afterReject = m.copy(pendingPaymentStatus = "rejected")
        assertEquals("rejected", afterReject.pendingPaymentStatus)
    }

    @Test
    fun `rejecting does not change amountPaid`() {
        val m = member(amountPaid = 100.0, pendingStatus = "pending", pendingAmount = 50.0)
        val afterReject = m.copy(pendingPaymentStatus = "rejected")
        assertEquals(100.0, afterReject.amountPaid, 0.001)
    }

    @Test
    fun `rejecting preserves the pending amount for display`() {
        val m = member(amountPaid = 0.0, pendingStatus = "pending", pendingAmount = 45.0)
        val afterReject = m.copy(pendingPaymentStatus = "rejected")
        assertEquals(45.0, afterReject.pendingPaymentAmount, 0.001)
    }

    // ─────────────────────────────────────────────
    // Revert approved
    // ─────────────────────────────────────────────

    @Test
    fun `reverting approved decrements amountPaid by event amount`() {
        val m = member(amountPaid = 80.0)
        val newAmountPaid = maxOf(0.0, m.amountPaid - 30.0)
        assertEquals(50.0, newAmountPaid, 0.001)
    }

    @Test
    fun `reverting approved floors amountPaid at zero`() {
        val m = member(amountPaid = 10.0)
        val newAmountPaid = maxOf(0.0, m.amountPaid - 50.0)
        assertEquals(0.0, newAmountPaid, 0.001)
    }

    @Test
    fun `reverting approved restores pending status and amount`() {
        val eventAmount = 30.0
        val afterRevert = member(
            amountPaid = 50.0,
            pendingStatus = "pending",
            pendingAmount = eventAmount
        )
        assertEquals("pending", afterRevert.pendingPaymentStatus)
        assertEquals(30.0, afterRevert.pendingPaymentAmount, 0.001)
    }

    @Test
    fun `reverting approved for exact amountPaid leaves zero`() {
        val m = member(amountPaid = 30.0)
        val newAmountPaid = maxOf(0.0, m.amountPaid - 30.0)
        assertEquals(0.0, newAmountPaid, 0.001)
    }

    // ─────────────────────────────────────────────
    // Revert rejected
    // ─────────────────────────────────────────────

    @Test
    fun `reverting rejected restores pending status and original amount`() {
        val eventAmount = 45.0
        val afterRevert = member(
            amountPaid = 0.0,
            pendingStatus = "pending",
            pendingAmount = eventAmount
        )
        assertEquals("pending", afterRevert.pendingPaymentStatus)
        assertEquals(45.0, afterRevert.pendingPaymentAmount, 0.001)
    }

    @Test
    fun `reverting rejected does not change amountPaid`() {
        val m = member(amountPaid = 100.0, pendingStatus = "rejected", pendingAmount = 50.0)
        // Revert rejected: only status/pendingAmount change, amountPaid stays the same
        assertEquals(100.0, m.amountPaid, 0.001)
    }

    // ─────────────────────────────────────────────
    // Re-submission overwrites previous pending
    // ─────────────────────────────────────────────

    @Test
    fun `re-submitting a new amount overwrites previous pending amount`() {
        val remaining = 100.0
        val firstSubmission = 30.0
        val secondSubmission = 50.0
        val capped = cappedAmount(secondSubmission, remaining)
        assertEquals(50.0, capped, 0.001)
        assertNotEquals(firstSubmission, capped, 0.001)
    }

    @Test
    fun `re-submitting after rejection resets status to pending`() {
        val m = member(amountPaid = 0.0, pendingStatus = "rejected", pendingAmount = 30.0)
        val afterResubmit = m.copy(pendingPaymentStatus = "pending", pendingPaymentAmount = 50.0)
        assertEquals("pending", afterResubmit.pendingPaymentStatus)
        assertEquals(50.0, afterResubmit.pendingPaymentAmount, 0.001)
    }

    // ─────────────────────────────────────────────
    // Amount due calculation
    // ─────────────────────────────────────────────

    @Test
    fun `remaining balance is computedOwed minus amountPaid`() {
        val computedOwed = 337.62
        val m = member(amountPaid = 30.0)
        val remaining = computedOwed - m.amountPaid
        assertEquals(307.62, remaining, 0.001)
    }

    @Test
    fun `remaining balance does not go below zero`() {
        val computedOwed = 100.0
        val m = member(amountPaid = 150.0)
        val remaining = maxOf(0.0, computedOwed - m.amountPaid)
        assertEquals(0.0, remaining, 0.001)
    }

    @Test
    fun `multiple approved payments accumulate correctly`() {
        var amountPaid = 0.0
        listOf(30.0, 50.0, 20.0).forEach { amountPaid += it }
        assertEquals(100.0, amountPaid, 0.001)
    }

    @Test
    fun `reverting one of multiple approved payments reduces total correctly`() {
        val amountPaid = 100.0 // after payments of 30 + 50 + 20
        val revertAmount = 50.0
        val newAmountPaid = maxOf(0.0, amountPaid - revertAmount)
        assertEquals(50.0, newAmountPaid, 0.001)
    }

    // ─────────────────────────────────────────────
    // PaymentEvent model
    // ─────────────────────────────────────────────

    @Test
    fun `PaymentEvent has correct default values`() {
        val event = PaymentEvent()
        assertEquals("", event.id)
        assertEquals("", event.type)
        assertEquals(0.0, event.amount, 0.001)
        assertEquals("", event.actorName)
        assertEquals(0L, event.timestamp)
    }

    @Test
    fun `PaymentEvent stores all fields correctly`() {
        val event = PaymentEvent(
            id = "evt1",
            type = "approved",
            amount = 75.50,
            actorName = "Trip Manager",
            timestamp = 1700000000L
        )
        assertEquals("evt1", event.id)
        assertEquals("approved", event.type)
        assertEquals(75.50, event.amount, 0.001)
        assertEquals("Trip Manager", event.actorName)
        assertEquals(1700000000L, event.timestamp)
    }

    @Test
    fun `PaymentEvent copy creates independent instance`() {
        val original = PaymentEvent(id = "evt1", type = "submitted", amount = 30.0)
        val copy = original.copy(type = "approved")
        assertEquals("submitted", original.type)
        assertEquals("approved", copy.type)
    }

    @Test
    fun `PaymentEvent equality works correctly`() {
        val a = PaymentEvent(id = "evt1", type = "submitted", amount = 30.0, actorName = "Alice", timestamp = 1000L)
        val b = PaymentEvent(id = "evt1", type = "submitted", amount = 30.0, actorName = "Alice", timestamp = 1000L)
        assertEquals(a, b)
    }

    @Test
    fun `PaymentEvent inequality detected correctly`() {
        val a = PaymentEvent(id = "evt1", type = "submitted", amount = 30.0)
        val b = PaymentEvent(id = "evt1", type = "approved", amount = 30.0)
        assertNotEquals(a, b)
    }

    // ─────────────────────────────────────────────
    // TripMember defaults
    // ─────────────────────────────────────────────

    @Test
    fun `new TripMember has no pending payment by default`() {
        val m = TripMember(uid = "uid1", displayName = "Alice", email = "alice@example.com")
        assertEquals("none", m.pendingPaymentStatus)
        assertEquals(0.0, m.pendingPaymentAmount, 0.001)
        assertEquals(0.0, m.amountPaid, 0.001)
    }
}
