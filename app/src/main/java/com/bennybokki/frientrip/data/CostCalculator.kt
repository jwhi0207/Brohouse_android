package com.bennybokki.frientrip.data

object CostCalculator {

    /**
     * Computes each member's share of the total trip cost.
     *
     * Since everyone starts on night 1, a guest staying N nights is present on nights 1..N.
     * For each night we split its cost equally among all guests still present
     * (nightsStayed >= that night number).
     *
     *   nightly_cost = totalCost / totalNights
     *   member share = Σ (nightly_cost / guests_present_that_night)
     *                    for each night in 1..member.nightsStayed
     *
     * Members with nightsStayed == 0 owe $0 until they set their nights.
     */
    fun computeCostSplit(
        members: List<TripMember>,
        totalNights: Int,
        totalCost: Double
    ): Map<String, Double> {
        if (totalNights <= 0 || totalCost <= 0.0 || members.isEmpty()) {
            return members.associate { it.uid to 0.0 }
        }
        val nightlyCost = totalCost / totalNights
        return members.associate { member ->
            val share = (1..member.nightsStayed).sumOf { night ->
                val presentCount = members.count { it.nightsStayed >= night }
                if (presentCount > 0) nightlyCost / presentCount else 0.0
            }
            member.uid to share
        }
    }

    /**
     * Computes total shares across house cost + approved shared expenses.
     *
     * House cost uses the existing per-night algorithm.
     * Each expense is split either evenly or by nights stayed.
     */
    fun computeTotalShares(
        members: List<TripMember>,
        totalNights: Int,
        houseCost: Double,
        approvedExpenses: List<SharedExpense>
    ): Map<String, Double> {
        if (members.isEmpty()) return emptyMap()

        val houseShares = computeCostSplit(members, totalNights, houseCost)
        val expenseShares = mutableMapOf<String, Double>()
        members.forEach { expenseShares[it.uid] = 0.0 }

        for (expense in approvedExpenses) {
            when (expense.splitMethod) {
                "even" -> {
                    val perMember = expense.amount / members.size
                    members.forEach { m ->
                        expenseShares[m.uid] = (expenseShares[m.uid] ?: 0.0) + perMember
                    }
                }
                "byNights" -> {
                    val nightShares = computeCostSplit(members, totalNights, expense.amount)
                    nightShares.forEach { (uid, share) ->
                        expenseShares[uid] = (expenseShares[uid] ?: 0.0) + share
                    }
                }
            }
        }

        return members.associate { m ->
            m.uid to (houseShares[m.uid] ?: 0.0) + (expenseShares[m.uid] ?: 0.0)
        }
    }
}
