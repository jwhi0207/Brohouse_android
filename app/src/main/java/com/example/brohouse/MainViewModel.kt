package com.example.brohouse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.brohouse.data.AppDatabase
import com.example.brohouse.data.HouseDetails
import com.example.brohouse.data.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val personDao = db.personDao()
    private val houseDetailsDao = db.houseDetailsDao()

    val people = personDao.getAllPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val houseDetails = houseDetailsDao.getHouseDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _saveComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveComplete = _saveComplete.asSharedFlow()

    fun addPerson(name: String) = viewModelScope.launch {
        personDao.insert(Person(name = name, avatarSeed = Random.nextLong()))
    }

    fun updateNights(person: Person, nights: Int) = viewModelScope.launch {
        personDao.update(person.copy(nightsStayed = nights))
    }

    fun addPayment(person: Person, amount: Double) = viewModelScope.launch {
        personDao.update(person.copy(moneyOwed = maxOf(0.0, person.moneyOwed - amount)))
    }

    fun saveHouseDetails(
        url: String,
        nights: Int,
        cost: Double,
        currentDetails: HouseDetails?
    ) = viewModelScope.launch {
        _isSaving.value = true
        val urlChanged = url != (currentDetails?.houseURL ?: "")
        val imageData = if (urlChanged && url.isNotBlank()) {
            withContext(Dispatchers.IO) { fetchOGImage(url) }
        } else {
            // Keep existing thumbnail if URL didn't change; clear if URL was blanked
            if (url.isBlank()) null else currentDetails?.thumbnailData
        }
        houseDetailsDao.upsert(
            HouseDetails(houseURL = url, totalNights = nights, totalCost = cost, thumbnailData = imageData)
        )
        _isSaving.value = false
        _saveComplete.emit(Unit)
    }

    private fun fetchOGImage(urlString: String): ByteArray? {
        return try {
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible)")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val html = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val imageUrlString = extractOGImageURL(html) ?: return null
            val resolved = when {
                imageUrlString.startsWith("//") -> "https:$imageUrlString"
                imageUrlString.startsWith("http") -> imageUrlString
                else -> URL(URL(urlString), imageUrlString).toString()
            }

            val imgConn = URL(resolved).openConnection() as HttpURLConnection
            imgConn.connectTimeout = 10_000
            imgConn.readTimeout = 10_000
            val bytes = imgConn.inputStream.readBytes()
            imgConn.disconnect()
            bytes
        } catch (e: Exception) {
            null
        }
    }

    private fun extractOGImageURL(html: String): String? {
        val patterns = listOf(
            Regex("""<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']""", RegexOption.IGNORE_CASE)
        )
        for (regex in patterns) {
            val match = regex.find(html)
            if (match != null) return match.groupValues[1]
        }
        return null
    }
}
