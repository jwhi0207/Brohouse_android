package com.bennybokki.frientrip.data

data class ClaimEntry(val name: String, val quantity: String)

data class SupplyItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val quantity: String = "",
    val claimedByUids: List<String> = emptyList(),
    val claimedByName: String? = null,
    val sortOrder: Int = 0
) {
    val claimedNames: List<String>
        get() = claimedByName?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    val isClaimed: Boolean get() = claimedNames.isNotEmpty()

    /** Per-person quantities parsed from the quantity field.
     *  Structured format: "Name=qty|Name2=qty2"
     *  Plain format (legacy/unclaimed): "nuff" */
    val claimEntries: List<ClaimEntry>
        get() {
            val names = claimedNames
            if (names.isEmpty()) return emptyList()
            val parsed = quantity.split("|").mapNotNull { entry ->
                val eq = entry.indexOf('=')
                if (eq > 0) ClaimEntry(entry.substring(0, eq).trim(), entry.substring(eq + 1).trim())
                else null
            }
            if (parsed.isNotEmpty() && parsed.any { it.name in names }) return parsed
            return names.map { ClaimEntry(it, quantity) }
        }

    val displayQuantity: String
        get() = if (!isClaimed) quantity
                else claimEntries.joinToString(", ") { "${it.name}: ${it.quantity}" }

    fun quantityForPerson(personName: String): String {
        return claimEntries.find { it.name == personName }?.quantity ?: quantity
    }

    fun addClaim(member: TripMember, personQuantity: String): SupplyItem {
        val currentUids = claimedByUids.toMutableList()
        if (member.uid !in currentUids) currentUids.add(member.uid)

        val currentNames = claimedNames.toMutableList()
        if (member.displayName !in currentNames) currentNames.add(member.displayName)

        val entries = claimEntries.toMutableList()
        entries.removeAll { it.name == member.displayName }
        entries.add(ClaimEntry(member.displayName, personQuantity))

        val newQuantity = entries.joinToString("|") { "${it.name}=${it.quantity}" }
        return copy(
            claimedByUids = currentUids,
            claimedByName = currentNames.joinToString(", "),
            quantity = newQuantity
        )
    }

    fun removeClaim(uid: String, displayName: String): SupplyItem {
        val currentUids = claimedByUids.toMutableList().also { it.remove(uid) }
        val currentNames = claimedNames.toMutableList().also { it.remove(displayName) }
        val entries = claimEntries.toMutableList().also { e -> e.removeAll { it.name == displayName } }
        return if (currentUids.isEmpty()) {
            copy(claimedByUids = emptyList(), claimedByName = null, quantity = "")
        } else {
            copy(
                claimedByUids = currentUids,
                claimedByName = currentNames.joinToString(", "),
                quantity = entries.joinToString("|") { "${it.name}=${it.quantity}" }
            )
        }
    }
}
