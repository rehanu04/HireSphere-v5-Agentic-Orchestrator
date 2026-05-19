package com.rehanu04.resumematchv2.util

import android.content.Context
import android.provider.Settings
import com.rehanu04.resumematchv2.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object SupabaseClient {
    // Keys are now securely hidden and pulled from local.properties!
    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY

    private val client = OkHttpClient()

    // Safely gets a unique ID for the phone so we don't need a login screen
    private fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    suspend fun backupVaultToCloud(context: Context, projects: String, experience: String, skills: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (SUPABASE_URL.isBlank() || SUPABASE_URL.contains("YOUR_SUPABASE")) return@withContext false

                val deviceId = getDeviceId(context)
                val jsonBody = JSONObject().apply {
                    put("id", deviceId)
                    put("projects", if (projects.isBlank()) "[]" else projects)
                    put("experience", if (experience.isBlank()) "[]" else experience)
                    put("skills", if (skills.isBlank()) "[]" else skills)
                }

                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/vault_profiles")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates") // Tells Supabase to UPSERT
                    .build()

                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun restoreVaultFromCloud(context: Context): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                if (SUPABASE_URL.isBlank() || SUPABASE_URL.contains("YOUR_SUPABASE")) return@withContext null
                val deviceId = getDeviceId(context)

                // The "?id=eq.$deviceId" part tells Supabase to only fetch this specific user's row
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/vault_profiles?id=eq.$deviceId&select=*")
                    .get()
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: "[]"
                        val jsonArray = org.json.JSONArray(responseBody)

                        // If the array has items, return the first row. Otherwise, return null.
                        if (jsonArray.length() > 0) jsonArray.getJSONObject(0) else null
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}