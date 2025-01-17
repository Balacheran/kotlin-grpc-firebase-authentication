package service_utils

import config.AppConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object TokenUtils {
    private val FIREBASE_API_KEY = AppConfig.firebaseWebApiKey
    private const val EXCHANGE_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken"

    suspend fun exchangeCustomTokenForIdToken(customToken: String): String = suspendCoroutine { continuation ->
        try {
            val client = OkHttpClient()
            val json = JSONObject()
                .put("token", customToken)
                .put("returnSecureToken", true)
            
            val requestBody = json.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$EXCHANGE_URL?key=$FIREBASE_API_KEY")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    println("Token exchange failed. Response: $errorBody")
                    throw Exception("Failed to exchange token: ${response.code}")
                }

                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody)
                val idToken = jsonResponse.getString("idToken")
                println("Successfully exchanged token")
                continuation.resume(idToken)
            }
        } catch (e: Exception) {
            println("Error exchanging token: ${e.message}")
            continuation.resumeWithException(e)
        }
    }
}