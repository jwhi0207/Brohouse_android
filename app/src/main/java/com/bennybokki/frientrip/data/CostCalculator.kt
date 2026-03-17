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
}
