package com.example.data.service

import com.example.data.model.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object OllamaService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    fun cleanUrl(url: String): String {
        var trimmed = url.trim()
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.dropLast(1)
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://$trimmed"
        }
        return trimmed
    }

    suspend fun testConnection(serverUrl: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val baseUrl = cleanUrl(serverUrl)
        val requestUrl = "$baseUrl/api/tags"

        val request = Request.Builder()
            .url(requestUrl)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val msg = response.message
                    return@withContext Result.failure(
                        Exception("Error del servidor Ollama (HTTP $code: $msg)")
                    )
                }

                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                val modelsArray = json.optJSONArray("models") ?: JSONArray()
                val modelNames = mutableListOf<String>()
                for (i in 0 until modelsArray.length()) {
                    val m = modelsArray.optJSONObject(i)
                    val name = m?.optString("name")
                    if (!name.isNullOrBlank()) {
                        modelNames.add(name)
                    }
                }
                Result.success(modelNames)
            }
        } catch (e: ConnectException) {
            Result.failure(Exception("No se pudo conectar con $baseUrl. Verifica que Ollama esté encendido y accesible desde tu celular."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Tiempo de espera agotado (60s) al conectar con $baseUrl."))
        } catch (e: UnknownHostException) {
            Result.failure(Exception("Dirección desconocida ($baseUrl). Verifica la IP o nombre de host."))
        } catch (e: IOException) {
            Result.failure(Exception("Error de red: ${e.localizedMessage ?: e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Error inesperado al probar conexión."))
        }
    }

    suspend fun suggestCategory(
        serverUrl: String,
        model: String,
        description: String,
        categories: List<CategoryEntity>
    ): Result<CategoryEntity?> = withContext(Dispatchers.IO) {
        if (description.isBlank() || categories.isEmpty()) {
            return@withContext Result.success(null)
        }

        val baseUrl = cleanUrl(serverUrl)
        val requestUrl = "$baseUrl/api/chat"
        val modelName = model.trim().ifBlank { "llama3.2:1b" }

        val categoryNames = categories.map { it.name }
        val prompt = "Descripción del movimiento: \"${description.trim()}\"\n" +
                "Categorías válidas permitidas: [${categoryNames.joinToString(", ")}]\n" +
                "Determina cuál de las categorías permitidas describe mejor este movimiento. " +
                "Responde estrictamente un JSON con la propiedad 'categoria' asignando el nombre exacto de la lista."

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "Eres un asistente de finanzas personales. Solo sugieres categorías de la lista proporcionada. Devuelve un objeto JSON con el formato: {\"categoria\":\"<nombre exacto>\"}.")
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val requestBodyJson = JSONObject().apply {
            put("model", modelName)
            put("stream", false)
            put("format", "json")
            put("messages", messagesArray)
        }

        val requestBody = requestBodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }

                val bodyStr = response.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                val messageObj = json.optJSONObject("message")
                val content = messageObj?.optString("content").orEmpty()

                if (content.isBlank()) {
                    return@withContext Result.success(null)
                }

                // Parse inner JSON response
                val innerJson = try {
                    JSONObject(content)
                } catch (_: Exception) {
                    // Try to find JSON substring if surrounded by extra text
                    val start = content.indexOf('{')
                    val end = content.lastIndexOf('}')
                    if (start != -1 && end != -1 && end > start) {
                        JSONObject(content.substring(start, end + 1))
                    } else null
                }

                val suggestedName = innerJson?.optString("categoria")?.trim()
                if (suggestedName.isNullOrBlank()) {
                    return@withContext Result.success(null)
                }

                // Match with existing categories (exact or case-insensitive)
                val matched = categories.firstOrNull {
                    it.name.trim().equals(suggestedName, ignoreCase = true)
                } ?: categories.firstOrNull {
                    suggestedName.contains(it.name.trim(), ignoreCase = true) ||
                            it.name.trim().contains(suggestedName, ignoreCase = true)
                }

                Result.success(matched)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
