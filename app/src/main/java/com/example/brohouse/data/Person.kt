package com.example.brohouse.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nightsStayed: Int = 0,
    val moneyOwed: Double = 0.0,
    val avatarSeed: Long = 0L
)

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY name ASC")
    fun getAllPersons(): Flow<List<Person>>

    @Insert
    suspend fun insert(person: Person)

    @Update
    suspend fun update(person: Person)

    @Delete
    suspend fun delete(person: Person)
}
