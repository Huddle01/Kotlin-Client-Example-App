package com.huddle01.kotlin_client_example_app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object TokenService {
    private const val apiKey = "ENTER_API_KEY"

    suspend fun getRoomId(): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.huddle01.com/api/v1/create-room")
            (url.openConnection() as? HttpURLConnection)?.run {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-api-key", apiKey)
                doOutput = true
                outputStream.use { os ->
                    os.write("""{"title": "room"}""".toByteArray(Charsets.UTF_8))
                }
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream.bufferedReader().use { br ->
                        val response = br.readText()
                        JSONObject(response).getJSONObject("data").getString("roomId")
                    }
                } else {
                    null
                }
            }
        } catch (error: Exception) {
            null
        }
    }

    suspend fun getAccessToken(role: Role, joinRoomId: String? = null): Map<String, String>? {
        val fetchRoomId = joinRoomId ?: getRoomId()
        Timber.i("fetchRoomId: $fetchRoomId")

        return fetchRoomId?.let {
            val url = "https://huddle01-token-simulator.vercel.app/api?apiKey=$apiKey&role=${role.name.lowercase(Locale.ROOT)}&roomId=$it"

            return@let runCatching {
                withContext(Dispatchers.IO) {
                    (URL(url).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("apiKey", apiKey)
                    }.let { connection ->
                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            val token = connection.inputStream.bufferedReader().readText()
                            mapOf("roomId" to it, "token" to token)
                        } else {
                            null
                        }
                    }
                }
            }.getOrElse { error ->
                Timber.e(error, "Failed to fetch access token")
                null
            }
        }
    }

}