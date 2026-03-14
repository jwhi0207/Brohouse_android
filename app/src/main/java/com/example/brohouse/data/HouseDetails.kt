package com.example.brohouse.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "house_details")
data class HouseDetails(
    @PrimaryKey val id: Int = 1,
    val houseURL: String = "",
    val totalNights: Int = 0,
    val totalCost: Double = 0.0,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val thumbnailData: ByteArray? = null
) {
    // ByteArray requires manual equals/hashCode for data class correctness
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HouseDetails) return false
        return id == other.id &&
                houseURL == other.houseURL &&
                totalNights == other.totalNights &&
                totalCost == other.totalCost &&
                (thumbnailData?.contentEquals(other.thumbnailData ?: byteArrayOf()) == true ||
                        (thumbnailData == null && other.thumbnailData == null))
    }

    override fun hashCode(): Int = id
}

@Dao
interface HouseDetailsDao {
    @Query("SELECT * FROM house_details WHERE id = 1")
    fun getHouseDetails(): Flow<HouseDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(details: HouseDetails)
}
