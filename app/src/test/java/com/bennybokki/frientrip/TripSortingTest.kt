package com.bennybokki.frientrip

import com.bennybokki.frientrip.data.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for trip list sorting and tab filter logic.
 *
 * Sorting mirrors TripListViewModel:
 *   val (withDate, withoutDate) = list.partition { it.checkInMillis > 0L }
 *   withDate.sortedBy { it.checkInMillis } + withoutDate.sortedBy { it.name.lowercase() }
 *
 * Tab filters mirror TripListScreen:
 *   Upcoming: checkOutMillis <= 0L || checkOutMillis >= now
 *   Past:     checkOutMillis > 0L  && checkOutMillis < now
 */
class TripSortingTest {

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun trip(name: String, checkIn: Long = 0L, checkOut: Long = 0L) =
        Trip(name = name, checkInMillis = checkIn, checkOutMillis = checkOut)

    private fun sortTrips(list: List<Trip>): List<Trip> {
        val (withDate, withoutDate) = list.partition { it.checkInMillis > 0L }
        return withDate.sortedBy { it.checkInMillis } + withoutDate.sortedBy { it.name.lowercase() }
    }

    private val now = 1_000_000L

    private fun upcomingTrips(trips: List<Trip>) =
        trips.filter { it.checkOutMillis <= 0L || it.checkOutMillis >= now }

    private fun pastTrips(trips: List<Trip>) =
        trips.filter { it.checkOutMillis > 0L && it.checkOutMillis < now }

    // ─── Sorting: dated trips ─────────────────────────────────────────────────

    @Test
    fun `trips with check-in dates sort earliest first`() {
        val trips = listOf(
            trip("Vegas", checkIn = 3000L),
            trip("Aspen", checkIn = 1000L),
            trip("Miami", checkIn = 2000L)
        )
        assertEquals(listOf("Aspen", "Miami", "Vegas"), sortTrips(trips).map { it.name })
    }

    @Test
    fun `single dated trip is unchanged`() {
        val trips = listOf(trip("Solo", checkIn = 1000L))
        assertEquals(listOf("Solo"), sortTrips(trips).map { it.name })
    }

    // ─── Sorting: undated trips ───────────────────────────────────────────────

    @Test
    fun `undated trips sort alphabetically`() {
        val trips = listOf(trip("Zion"), trip("Austin"), trip("Miami"))
        assertEquals(listOf("Austin", "Miami", "Zion"), sortTrips(trips).map { it.name })
    }

    @Test
    fun `alphabetical sort for undated trips is case-insensitive`() {
        val trips = listOf(trip("zoo trip"), trip("Apple Orchard"), trip("beach house"))
        assertEquals(
            listOf("Apple Orchard", "beach house", "zoo trip"),
            sortTrips(trips).map { it.name }
        )
    }

    // ─── Sorting: mixed dated and undated ────────────────────────────────────

    @Test
    fun `dated trips appear before undated trips`() {
        val trips = listOf(
            trip("No Date A"),
            trip("Dated", checkIn = 1000L),
            trip("No Date B")
        )
        val sorted = sortTrips(trips)
        assertEquals("Dated", sorted.first().name)
        assertEquals(listOf("No Date A", "No Date B"), sorted.drop(1).map { it.name })
    }

    @Test
    fun `undated trips are alphabetical after all dated trips`() {
        val trips = listOf(
            trip("Zion"),
            trip("C Trip", checkIn = 3000L),
            trip("Arches"),
            trip("A Trip", checkIn = 1000L),
            trip("Mesa"),
            trip("B Trip", checkIn = 2000L)
        )
        val sorted = sortTrips(trips)
        assertEquals(listOf("A Trip", "B Trip", "C Trip"), sorted.take(3).map { it.name })
        assertEquals(listOf("Arches", "Mesa", "Zion"), sorted.drop(3).map { it.name })
    }

    @Test
    fun `empty list returns empty`() {
        assertEquals(emptyList<Trip>(), sortTrips(emptyList()))
    }

    // ─── Tab filter: Upcoming ─────────────────────────────────────────────────

    @Test
    fun `upcoming includes trips with no checkout date`() {
        assertEquals(1, upcomingTrips(listOf(trip("No Checkout"))).size)
    }

    @Test
    fun `upcoming includes trips with a future checkout date`() {
        assertEquals(1, upcomingTrips(listOf(trip("Future", checkOut = now + 1))).size)
    }

    @Test
    fun `upcoming includes trips checking out exactly now`() {
        assertEquals(1, upcomingTrips(listOf(trip("Today", checkOut = now))).size)
    }

    @Test
    fun `upcoming excludes trips with a past checkout date`() {
        assertEquals(0, upcomingTrips(listOf(trip("Past", checkOut = now - 1))).size)
    }

    // ─── Tab filter: Past ─────────────────────────────────────────────────────

    @Test
    fun `past includes trips whose checkout date has passed`() {
        assertEquals(1, pastTrips(listOf(trip("Past", checkOut = now - 1))).size)
    }

    @Test
    fun `past excludes trips with no checkout date`() {
        assertEquals(0, pastTrips(listOf(trip("No Checkout"))).size)
    }

    @Test
    fun `past excludes trips with a future checkout date`() {
        assertEquals(0, pastTrips(listOf(trip("Future", checkOut = now + 1))).size)
    }

    @Test
    fun `past excludes trips checking out exactly now`() {
        assertEquals(0, pastTrips(listOf(trip("Today", checkOut = now))).size)
    }

    // ─── Tab filter: mutual exclusivity ──────────────────────────────────────

    @Test
    fun `upcoming and past together cover all trips exactly once`() {
        val trips = listOf(
            trip("No Date"),
            trip("Future", checkOut = now + 500),
            trip("Past", checkOut = now - 500),
            trip("Today", checkOut = now)
        )
        val upcoming = upcomingTrips(trips)
        val past = pastTrips(trips)

        assertTrue(upcoming.intersect(past.toSet()).isEmpty())
        assertEquals(trips.size, upcoming.size + past.size)
    }
}
