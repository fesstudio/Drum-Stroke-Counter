package com.drummer.speed.data.repository

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor() {
    
    suspend fun checkForUpdates(): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val versionJsonUrl = "https://raw.githubusercontent.com/USERNAME_ANDA/drum-stroke-counter/main/version.json"
            val jsonContent = URL(versionJsonUrl).readText()
            val gson = Gson()
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(jsonContent, Map::class.java) as? Map<String, Any>
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
