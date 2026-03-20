package com.bennybokki.frientrip

import com.bennybokki.frientrip.data.CostCalculator
import com.bennybokki.frientrip.data.SharedExpense
import com.bennybokki.frientrip.data.TripMember
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CostCalculatorTest {

    private fun member(uid: String, nights: Int) = TripMember(
        uid = uid,
        displayName = uid,
        email = "$uid@test.com",
        avatarSeed = 0L,
        nightsStayed = nights,
        amountPaid = 0.0
    )

    // ─── Guard conditions ────────────────────────────────────────────────────

    @Test
    fun `returns zero for all members when totalNights is zero`() {
        val members = listOf(member("a", 3), member("b", 2))
        val result = CostCalculator.computeCostSplit(members, totalNights = 0, totalCost = 300.0)
        assertEquals(0.0, result["a"]!!, 0.001)
        assertEquals(0.0, result["b"]!!, 0.001)
    }

    @Test
    fun `returns zero for all members when totalCost is zero`() {
        val members = listOf(member("a", 3), member("b", 2))
        val result = CostCalculator.computeCostSplit(members, totalNights = 3, totalCost = 0.0)
        assertEquals(0.0, result["a"]!!, 0.001)
        assertEquals(0.0, result["b"]!!, 0.001)
    }

    @Test
    fun `returns empty map when members list is empty`() {
        val result = CostCalculator.computeCostSplit(emptyList(), totalNights = 3, totalCost = 300.0)
        assertEquals(emptyMap<String, Double>(), result)
    }

    @Test
    fun `member with zero nights owes nothing`() {
        val members = listOf(member("a", 3), member("b", 0))
        val result = CostCalculator.computeCostSplit(members, totalNights = 3, totalCost = 300.0)
        assertEquals(0.0, result["b"]!!, 0.001)
    }

    // ─── Equal stays ─────────────────────────────────────────────────────────

    @Test
    fun `two members same nights split equally`() {
        // 2 members, each stays 3 nights, total cost $300, 3 nights
        // nightly = 100, each night 2 present → 50 per person per night
        // each person: 3 * 50 = 150
        val members = listOf(member("a", 3), member("b", 3))
        val result = CostCalculator.computeCostSplit(members, totalNights = 3, totalCost = 300.0)
        assertEquals(150.0, result["a"]!!, 0.001)
        assertEquals(150.0, result["b"]!!, 0.001)
    }

    @Test
    fun `single member pays full cost`() {
        val members = listOf(member("a", 3))
        val result = CostCalculator.computeCostSplit(members, totalNights = 3, totalCost = 300.0)
        assertEquals(300.0, result["a"]!!, 0.001)
    }

    // ─── Unequal stays ───────────────────────────────────────────────────────

    @Test
    fun `member leaving early pays less`() {
        // 2 members: a stays 3 nights, b stays 1 night, total cost $300, 3 nights
        // nightly = 100
        // Night 1: both present (2 guests) → each pays 50
        // Night 2: only a present (1 guest) → a pays 100
        // Night 3: only a present (1 guest) → a pays 100
        // a total: 50 + 100 + 100 = 250
        // b total: 50
        val members = listOf(member("a", 3), member("b", 1))
        val result = CostCalculator.computeCostSplit(members, totalNights = 3, totalCost = 300.0)
        assertEquals(250.0, result["a"]!!, 0.001)
        assertEquals(50.0, result["b"]!!, 0.001)
    }

    @Test
    fun `three members different nights`() {
        // a=3 nights, b=2 nights, c=1 night, total 3 nights, cost $180
        // nightly = 60
        // Night 1: 3 present → each 20
        // Night 2: a,b present → each 30
        // Night 3: a only → a pays 60
        // a: 20 + 30 + 60 = 110
        // b: 20 + 30 = 50
        // c: 20
        val members = listOf(member("a", 3), member("b", 2), member("c", 1))
        val result = CostCalculator.computeCostSplit(members, totalNights = 3, totalCost = 180.0)
        assertEquals(110.0, result["a"]!!, 0.001)
        assertEquals(50.0, result["b"]!!, 0.001)
        assertEquals(20.0, result["c"]!!, 0.001)
    }

    @Test
    fun `cost shares sum to total cost`() {
        val members = listOf(member("a", 5), member("b", 3), member("c", 2), member("d", 1))
        val totalCost = 500.0
        val result = CostCalculator.computeCostSplit(members, totalNights = 5, totalCost = totalCost)
        val sum = result.values.sum()
        assertEquals(totalCost, sum, 0.001)
    }

    // ─── computeTotalShares ────────────────────────────────────────────────────

    private fun expense(
        amount: Double,
        splitMethod: String = "even",
        approved: Boolean = true
    ) = SharedExpense(
        id = "e${amount.toInt()}",
        description = "Test expense",
        amount = amount,
        splitMethod = splitMethod,
        submittedByUid = "a",
        submittedByName = "a",
        approved = approved,
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun `totalShares with no expenses equals house-only split`() {
        val members = listOf(member("a", 3), member("b", 3))
        val houseOnly = CostCalculator.computeCostSplit(members, 3, 300.0)
        val total = CostCalculator.computeTotalShares(members, 3, 300.0, emptyList())
        assertEquals(houseOnly["a"]!!, total["a"]!!, 0.001)
        assertEquals(houseOnly["b"]!!, total["b"]!!, 0.001)
    }

    @Test
    fun `even split expense divides equally among members`() {
        val members = listOf(member("a", 3), member("b", 3))
        val expenses = listOf(expense(100.0, "even"))
        val result = CostCalculator.computeTotalShares(members, 3, 0.0, expenses)
        // $100 / 2 members = $50 each
        assertEquals(50.0, result["a"]!!, 0.001)
        assertEquals(50.0, result["b"]!!, 0.001)
    }

    @Test
    fun `byNights split expense uses night-based algorithm`() {
        val members = listOf(member("a", 3), member("b", 1))
        val expenses = listOf(expense(300.0, "byNights"))
        val result = CostCalculator.computeTotalShares(members, 3, 0.0, expenses)
        // Same as computeCostSplit: a=250, b=50
        assertEquals(250.0, result["a"]!!, 0.001)
        assertEquals(50.0, result["b"]!!, 0.001)
    }

    @Test
    fun `house cost plus even expense combines correctly`() {
        val members = listOf(member("a", 3), member("b", 3))
        val expenses = listOf(expense(60.0, "even"))
        val result = CostCalculator.computeTotalShares(members, 3, 300.0, expenses)
        // House: 150 each. Expense: 30 each. Total: 180 each.
        assertEquals(180.0, result["a"]!!, 0.001)
        assertEquals(180.0, result["b"]!!, 0.001)
    }

    @Test
    fun `multiple expenses accumulate`() {
        val members = listOf(member("a", 2), member("b", 2))
        val expenses = listOf(
            expense(100.0, "even"),
            expense(40.0, "even"),
            expense(60.0, "even")
        )
        val result = CostCalculator.computeTotalShares(members, 2, 0.0, expenses)
        // (100 + 40 + 60) / 2 = 100 each
        assertEquals(100.0, result["a"]!!, 0.001)
        assertEquals(100.0, result["b"]!!, 0.001)
    }

    @Test
    fun `totalShares sums to house cost plus all expense amounts`() {
        val members = listOf(member("a", 5), member("b", 3), member("c", 2))
        val houseCost = 500.0
        val expenses = listOf(
            expense(120.0, "even"),
            expense(80.0, "byNights")
        )
        val result = CostCalculator.computeTotalShares(members, 5, houseCost, expenses)
        val sum = result.values.sum()
        assertEquals(houseCost + 120.0 + 80.0, sum, 0.001)
    }

    @Test
    fun `empty members returns empty map for totalShares`() {
        val result = CostCalculator.computeTotalShares(emptyList(), 3, 300.0, listOf(expense(100.0)))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `member with zero nights gets even expense share but no house or byNights share`() {
        val members = listOf(member("a", 3), member("b", 0))
        val expenses = listOf(expense(100.0, "even"))
        val result = CostCalculator.computeTotalShares(members, 3, 300.0, expenses)
        // b: house=0 (0 nights), even expense=50. Total=50
        assertEquals(50.0, result["b"]!!, 0.001)
        // a: house=300 (only one staying), even expense=50. Total=350
        assertEquals(350.0, result["a"]!!, 0.001)
    }
}
