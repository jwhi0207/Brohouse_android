package com.example.brohouse.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class ClaimEntry(val name: String, val quantity: String)

@Entity(tableName = "supply_items")
data class SupplyItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val quantity: String = "",
    val claimedByPersonId: Long? = null,
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
            // Try structured parse
            val parsed = quantity.split("|").mapNotNull { entry ->
                val eq = entry.indexOf('=')
                if (eq > 0) ClaimEntry(entry.substring(0, eq).trim(), entry.substring(eq + 1).trim())
                else null
            }
            if (parsed.isNotEmpty() && parsed.any { it.name in names }) return parsed
            // Fallback: shared quantity
            return names.map { ClaimEntry(it, quantity) }
        }

    /** The plain quantity string for display when unclaimed */
    val displayQuantity: String
        get() = if (!isClaimed) quantity
                else claimEntries.joinToString(", ") { "${it.name}: ${it.quantity}" }

    fun quantityForPerson(personName: String): String {
        return claimEntries.find { it.name == personName }?.quantity ?: quantity
    }

    fun addClaim(person: Person, personQuantity: String): SupplyItem {
        val currentNames = claimedNames.toMutableList()
        if (person.name !in currentNames) currentNames.add(person.name)
        // Build per-person quantity entries
        val entries = claimEntries.toMutableList()
        entries.removeAll { it.name == person.name }
        if (personQuantity.isNotBlank()) {
            entries.add(ClaimEntry(person.name, personQuantity))
        } else {
            entries.add(ClaimEntry(person.name, ""))
        }
        val newQuantity = entries.joinToString("|") { "${it.name}=${it.quantity}" }
        return copy(
            claimedByPersonId = person.id,
            claimedByName = currentNames.joinToString(", "),
            quantity = newQuantity
        )
    }

    fun removeClaim(personName: String): SupplyItem {
        val currentNames = claimedNames.toMutableList()
        currentNames.remove(personName)
        val entries = claimEntries.toMutableList()
        entries.removeAll { it.name == personName }
        return if (currentNames.isEmpty()) {
            copy(claimedByPersonId = null, claimedByName = null, quantity = "")
        } else {
            copy(
                claimedByName = currentNames.joinToString(", "),
                quantity = entries.joinToString("|") { "${it.name}=${it.quantity}" }
            )
        }
    }
}

@Dao
interface SupplyItemDao {
    @Query("SELECT * FROM supply_items ORDER BY category ASC, sortOrder ASC, name ASC")
    fun getAllSupplyItems(): Flow<List<SupplyItem>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM supply_items WHERE category = :category")
    suspend fun getMaxSortOrder(category: String): Int

    @Insert
    suspend fun insert(item: SupplyItem)

    @Update
    suspend fun update(item: SupplyItem)

    @Update
    suspend fun updateAll(items: List<SupplyItem>)

    @Delete
    suspend fun delete(item: SupplyItem)
}
