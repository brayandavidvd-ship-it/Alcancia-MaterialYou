package com.example.data.service

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
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GeminiFunctionCallItem(
    val name: String,
    val arguments: Map<String, Any?>,
    val isQuery: Boolean
)

sealed class GeminiTurnResult {
    data class TextResponse(
        val text: String,
        val rawModelParts: List<JSONObject> = emptyList()
    ) : GeminiTurnResult()

    data class QueryFunctionCall(
        val callId: String,
        val functionName: String,
        val arguments: Map<String, Any?>,
        val allCalls: List<GeminiFunctionCallItem>,
        val rawModelParts: List<JSONObject>
    ) : GeminiTurnResult()

    data class ActionProposalCall(
        val proposalId: String,
        val toolName: String,
        val summary: String,
        val isDestructive: Boolean,
        val arguments: Map<String, Any?>,
        val allCalls: List<GeminiFunctionCallItem>,
        val rawModelParts: List<JSONObject>
    ) : GeminiTurnResult()

    data class Error(
        val message: String,
        val technicalDetail: String? = null,
        val statusCode: Int? = null,
        val isTurnOrderError: Boolean = false
    ) : GeminiTurnResult()
}

data class GeminiMessage(
    val role: String, // "user" or "model"
    val parts: List<JSONObject>
) {
    fun isUser(): Boolean = role == "user"
    fun isModel(): Boolean = role == "model"

    fun getFunctionCallNames(): List<String> {
        val list = mutableListOf<String>()
        for (part in parts) {
            if (part.has("functionCall")) {
                val n = part.getJSONObject("functionCall").optString("name", "")
                if (n.isNotBlank()) list.add(n)
            }
        }
        return list
    }

    fun getFunctionResponseNames(): List<String> {
        val list = mutableListOf<String>()
        for (part in parts) {
            if (part.has("functionResponse")) {
                val n = part.getJSONObject("functionResponse").optString("name", "")
                if (n.isNotBlank()) list.add(n)
            }
        }
        return list
    }

    fun hasFunctionCall(): Boolean = getFunctionCallNames().isNotEmpty()
    fun hasFunctionResponse(): Boolean = getFunctionResponseNames().isNotEmpty()
}

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
    const val FLASH_MODEL = "gemini-3.5-flash"

    val VALID_MODELS = setOf(DEFAULT_MODEL, FLASH_MODEL)

    fun normalizeModel(model: String?): String {
        val trimmed = model?.trim().orEmpty()
        return if (trimmed == FLASH_MODEL) FLASH_MODEL else DEFAULT_MODEL
    }

    /**
     * Tests the API key and model against Gemini Generative Language API.
     */
    suspend fun testConnection(apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Clave de API inválida o no configurada."))
        }
        val cleanModel = normalizeModel(model)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=${apiKey.trim()}"

        val bodyJson = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "Responde solo con la palabra: OK") })
                    })
                })
            }
            put("contents", contents)
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                when {
                    response.isSuccessful -> {
                        Result.success("Conexión exitosa con Gemini ($cleanModel)")
                    }
                    response.code == 401 || response.code == 403 || responseBody.contains("API_KEY_INVALID", ignoreCase = true) -> {
                        Result.failure(Exception("Clave de API inválida o no configurada."))
                    }
                    response.code == 400 -> {
                        val errorMsg = try {
                            val json = JSONObject(responseBody)
                            json.optJSONObject("error")?.optString("message") ?: "Parámetros de solicitud incorrectos."
                        } catch (_: Exception) {
                            "Parámetros de solicitud incorrectos."
                        }
                        Result.failure(Exception("Error de la solicitud: $errorMsg"))
                    }
                    response.code == 429 || responseBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) -> {
                        Result.failure(Exception("Llegaste al límite gratuito, intenta más tarde."))
                    }
                    else -> {
                        val errorMsg = try {
                            val json = JSONObject(responseBody)
                            json.optJSONObject("error")?.optString("message") ?: "Error ${response.code}"
                        } catch (_: Exception) {
                            "Error ${response.code}"
                        }
                        Result.failure(Exception("Error del servidor (${response.code}): $errorMsg"))
                    }
                }
            }
        } catch (e: UnknownHostException) {
            Result.failure(Exception("No hay conexión a internet. Revisa tu conexión."))
        } catch (e: ConnectException) {
            Result.failure(Exception("No hay conexión a internet. Revisa tu conexión."))
        } catch (e: IOException) {
            Result.failure(Exception("Tiempo de espera agotado o fallo de red. Revisa tu conexión."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Error desconocido"))
        }
    }

    /**
     * Executes a chat round with Gemini Generative Language API.
     */
    suspend fun generateContent(
        apiKey: String,
        model: String,
        currencySymbol: String,
        history: List<GeminiMessage>
    ): GeminiTurnResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiTurnResult.Error("Clave de API inválida o no configurada.")
        }
        val cleanModel = normalizeModel(model)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=${apiKey.trim()}"

        // System instructions (Enhanced for natural language comprehension & exact tool usage)
        val systemInstruction = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put(
                        "text",
                        """Eres el asistente de Alcancía, una app de finanzas personales para Android. Responde siempre corto, claro y en español, usando la moneda configurada ($currencySymbol).

REGLA CRÍTICA - NUNCA INVENTAR DATOS:
Nunca muestres una tarjeta con un monto, nombre u otro dato imprescindible que el usuario no haya dicho. Si falta, pregunta solo ese dato antes de mostrar la tarjeta.
Si el usuario dice 'agrega dinero a mi meta' o 'agrega un gasto' o similar sin decir cuánto, pregunta únicamente en una sola frase breve: '¿Cuánto dinero quieres agregar a la meta?' o '¿De cuánto es el gasto?', sin proponer montos por tu cuenta ni mostrar tarjetas hasta tener el dato confirmado.

IDENTIFICADORES:
Jamás muestres identificadores técnicos ni '#número' en ningún lugar (ni en las tarjetas ni en los mensajes del asistente). Describe siempre a qué se refiere con su tipo, descripción o concepto, monto, categoría y fecha (ej. 'Ingreso de $100 del 9 de nov', 'Gasto de $50 en Alimentos del 10 de nov').

COMPRENSIÓN DEL LENGUAJE:
- Sinónimos de acciones:
  * "quitar", "eliminar", "borrar", "sacar" = eliminar.
  * "agregar", "añadir", "registrar", "apuntar", "poner" = crear.
  * "cambiar", "corregir", "modificar", "actualizar" = editar.
- Para editar o eliminar algo sin que el usuario dé todos los datos (ej. "quita el gasto de café", "borra el ingreso de 1500"):
  * Primero usa las herramientas de lectura ('consultar_movimientos', 'listar_metas', 'listar_compras', 'listar_categorias') para localizar el elemento coincidente por monto, descripción, categoría o fecha.
  * Si hay una sola coincidencia clara, ve DIRECTO a la tarjeta de confirmación llamando la herramienta correspondiente ('eliminar_movimiento', 'editar_movimiento', etc.).
  * Si hay varias coincidencias, muestra la lista numerada al usuario y pregúntale cuál (describiendo tipo, concepto, monto y fecha, NUNCA mostrando IDs técnicos al usuario).
  * Cuando el usuario responda "el primero", "el de 100", "el segundo", "ese", resuélvelo con la lista que se acaba de consultar.
- Si falta un dato imprescindible (monto, nombre o concepto), pregunta ÚNICAMENTE ese dato faltante en una sola frase breve. Si el pedido es ambiguo, pregunta; NUNCA inventes datos.

REGLAS DE HERRAMIENTAS Y CONFIRMACIÓN:
1. METAS DE AHORRO:
   - Para crear una nueva meta usa EXCLUSIVAMENTE 'crear_meta'. NUNCA uses 'agregar_movimiento'.
   - Para aportar dinero a una meta usa 'aportar_a_meta'. Para retirar dinero ahorrado usa 'retirar_de_meta'.
   - Los aportes a metas descuentan del saldo mensual disponible sin contar como gasto de consumo.
   - Para editar o eliminar usa 'editar_meta' o 'eliminar_meta'. Al eliminar una meta con fondos, ese dinero regresa al saldo disponible y la tarjeta de confirmación lo indicará.
2. MOVIMIENTOS:
   - Para gastos o ingresos usa 'agregar_movimiento', 'editar_movimiento' o 'eliminar_movimiento'.
   - Siempre asigna la categoría correspondiente. Si no tiene categoría clara, déjala null (se mostrará como Sin categoría). NUNCA asignes 'General' si el movimiento corresponde a otra categoría clara.
3. LISTAS DE COMPRAS:
   - Usa 'listar_compras', 'crear_lista_compras', 'agregar_producto_compra', 'editar_producto_compra', 'eliminar_producto_compra'.
4. CONFIRMACIÓN:
   - Toda acción que cree, modifique o borre datos mostrará una tarjeta en el chat para confirmación del usuario con datos reales (tipo, monto, categoría real, fecha en formato corto).
5. BORRADOS MASIVOS:
   - Para 'borrar_todos_los_movimientos', solicita confirmación explícita con el total exacto."""
                    )
                })
            })
        }

        // Section 3.g: Sanitize history cleanly preserving complete pairs (no arbitrary slicing)
        val sanitizedHistory = GeminiHistorySanitizer.sanitize(history)

        val contentsJson = JSONArray()
        for (msg in sanitizedHistory) {
            val msgObj = JSONObject()
            msgObj.put("role", msg.role)
            val partsArr = JSONArray()
            for (p in msg.parts) {
                partsArr.put(p)
            }
            msgObj.put("parts", partsArr)
            contentsJson.put(msgObj)
        }

        // Section 3.h: Logcat diagnostic trace before each request
        val seqSummary = sanitizedHistory.mapIndexed { idx, m ->
            val pTypes = m.parts.map { p ->
                when {
                    p.has("text") -> "text"
                    p.has("functionCall") -> "call(${p.optJSONObject("functionCall")?.optString("name")})"
                    p.has("functionResponse") -> "resp(${p.optJSONObject("functionResponse")?.optString("name")})"
                    else -> "part"
                }
            }.joinToString(",")
            "[$idx:${m.role}:$pTypes]"
        }.joinToString(" -> ")
        android.util.Log.d("GeminiHistory", "generateContent model=$cleanModel turns=${sanitizedHistory.size}: $seqSummary")

        val toolsJson = JSONArray().apply {
            put(JSONObject().apply {
                put("function_declarations", getFunctionDeclarations(currencySymbol))
            })
        }

        val requestJson = JSONObject().apply {
            put("system_instruction", systemInstruction)
            put("contents", contentsJson)
            put("tools", toolsJson)
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                when {
                    response.isSuccessful -> {
                        parseResponse(responseBody, currencySymbol)
                    }
                    response.code == 401 || response.code == 403 || responseBody.contains("API_KEY_INVALID", ignoreCase = true) -> {
                        GeminiTurnResult.Error(
                            message = "Clave de API inválida o no configurada.",
                            technicalDetail = "HTTP ${response.code}: $responseBody",
                            statusCode = response.code
                        )
                    }
                    response.code == 400 -> {
                        val errorMsg = try {
                            val json = JSONObject(responseBody)
                            json.optJSONObject("error")?.optString("message") ?: "Parámetros de solicitud incorrectos."
                        } catch (_: Exception) {
                            "Parámetros de solicitud incorrectos."
                        }
                        val isTurnOrder = errorMsg.contains("turn", ignoreCase = true) ||
                                          errorMsg.contains("function", ignoreCase = true) ||
                                          errorMsg.contains("role", ignoreCase = true)
                        GeminiTurnResult.Error(
                            message = "Error de la solicitud: $errorMsg",
                            technicalDetail = "HTTP 400: $errorMsg\n\nDetalle completo:\n$responseBody",
                            statusCode = 400,
                            isTurnOrderError = isTurnOrder
                        )
                    }
                    response.code == 429 || responseBody.contains("RESOURCE_EXHAUSTED", ignoreCase = true) -> {
                        GeminiTurnResult.Error(
                            message = "Llegaste al límite gratuito de solicitudes, intenta más tarde.",
                            technicalDetail = "HTTP 429: $responseBody",
                            statusCode = 429
                        )
                    }
                    else -> {
                        val errorMsg = try {
                            val json = JSONObject(responseBody)
                            json.optJSONObject("error")?.optString("message") ?: "Error ${response.code}"
                        } catch (_: Exception) {
                            "Error ${response.code}"
                        }
                        GeminiTurnResult.Error(
                            message = "Error del servidor (${response.code}): $errorMsg",
                            technicalDetail = "HTTP ${response.code}: $responseBody",
                            statusCode = response.code
                        )
                    }
                }
            }
        } catch (e: UnknownHostException) {
            GeminiTurnResult.Error(
                message = "No hay conexión a internet. Revisa tu conexión.",
                technicalDetail = e.message ?: e.toString(),
                statusCode = -1
            )
        } catch (e: ConnectException) {
            GeminiTurnResult.Error(
                message = "No hay conexión a internet. Revisa tu conexión.",
                technicalDetail = e.message ?: e.toString(),
                statusCode = -1
            )
        } catch (e: IOException) {
            GeminiTurnResult.Error(
                message = "Tiempo de espera agotado o fallo de conexión. Revisa tu red.",
                technicalDetail = e.message ?: e.toString(),
                statusCode = -2
            )
        } catch (e: Exception) {
            GeminiTurnResult.Error(
                message = e.message ?: "Error al procesar la solicitud.",
                technicalDetail = e.stackTraceToString(),
                statusCode = -3
            )
        }
    }

    private fun parseResponse(jsonString: String, currencySymbol: String): GeminiTurnResult {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return GeminiTurnResult.TextResponse("No se recibió respuesta del modelo.")
            }
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val partsArr = content?.optJSONArray("parts")
            if (partsArr == null || partsArr.length() == 0) {
                return GeminiTurnResult.TextResponse("Respuesta vacía.")
            }

            // CRITICAL: Keep all raw model parts intact with their thoughtSignature for Gemini 3
            val rawPartsList = mutableListOf<JSONObject>()
            for (i in 0 until partsArr.length()) {
                rawPartsList.add(partsArr.getJSONObject(i))
            }

            val functionCallItems = mutableListOf<GeminiFunctionCallItem>()
            var textResponse = ""
            for (part in rawPartsList) {
                if (part.has("functionCall")) {
                    val fnCall = part.getJSONObject("functionCall")
                    val name = fnCall.getString("name")
                    val argsObj = fnCall.optJSONObject("args") ?: JSONObject()
                    val argsMap = mutableMapOf<String, Any?>()
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.get(key)
                    }
                    functionCallItems.add(
                        GeminiFunctionCallItem(
                            name = name,
                            arguments = argsMap,
                            isQuery = isQueryTool(name)
                        )
                    )
                } else if (part.has("text")) {
                    textResponse += part.getString("text")
                }
            }

            if (functionCallItems.isNotEmpty()) {
                val firstAction = functionCallItems.firstOrNull { !it.isQuery }
                if (firstAction == null) {
                    val firstQuery = functionCallItems.first()
                    return GeminiTurnResult.QueryFunctionCall(
                        callId = "call_${System.currentTimeMillis()}",
                        functionName = firstQuery.name,
                        arguments = firstQuery.arguments,
                        allCalls = functionCallItems,
                        rawModelParts = rawPartsList
                    )
                } else {
                    val summary = buildActionSummary(firstAction.name, firstAction.arguments, currencySymbol)
                    val isDestructive = firstAction.name.startsWith("eliminar_") || firstAction.name.startsWith("borrar_")
                    return GeminiTurnResult.ActionProposalCall(
                        proposalId = "prop_${System.currentTimeMillis()}",
                        toolName = firstAction.name,
                        summary = summary,
                        isDestructive = isDestructive,
                        arguments = firstAction.arguments,
                        allCalls = functionCallItems,
                        rawModelParts = rawPartsList
                    )
                }
            }

            GeminiTurnResult.TextResponse(textResponse.trim(), rawModelParts = rawPartsList)
        } catch (e: Exception) {
            GeminiTurnResult.Error("Error al interpretar respuesta: ${e.message}", technicalDetail = e.stackTraceToString())
        }
    }

    private fun isQueryTool(name: String): Boolean {
        return name in setOf(
            "consultar_movimientos",
            "consultar_resumen",
            "listar_categorias",
            "listar_metas",
            "listar_compras"
        )
    }

    private fun formatShortDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank() || rawDate.equals("hoy", ignoreCase = true)) {
            val sdf = SimpleDateFormat("d 'de' MMM", Locale("es", "MX"))
            return sdf.format(Date()).replace(".", "")
        }
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = parser.parse(rawDate)
            if (date != null) {
                SimpleDateFormat("d 'de' MMM", Locale("es", "MX")).format(date).replace(".", "")
            } else {
                rawDate
            }
        } catch (_: Exception) {
            rawDate
        }
    }

    private fun buildActionSummary(name: String, args: Map<String, Any?>, currencySymbol: String): String {
        return when (name) {
            "agregar_movimiento" -> {
                val tipo = when (args["tipo"]?.toString()?.uppercase()) {
                    "INCOME" -> "Ingreso"
                    "SAVING_DEPOSIT" -> "Ahorro"
                    else -> "Gasto"
                }
                val monto = args["monto"]?.toString()?.toDoubleOrNull() ?: 0.0
                val formattedAmount = "$currencySymbol${String.format(Locale.US, "%.2f", monto)}"
                val rawCat = args["categoria"]?.toString()
                val categoria = if (rawCat.isNullOrBlank() || rawCat.contains("actual", ignoreCase = true)) "Sin categoría" else rawCat
                val nota = args["nota"]?.toString()?.takeIf { it.isNotBlank() && !it.equals("Sin nota", ignoreCase = true) }
                val fecha = formatShortDate(args["fecha"]?.toString())
                val hora = args["hora"]?.toString()?.takeIf { it.isNotBlank() }?.let { " a las $it" } ?: ""
                val notaPart = if (nota != null) " en '$nota'" else ""
                "¿Confirmas registrar este $tipo de $formattedAmount$notaPart (Categoría: $categoria, Fecha: $fecha$hora)?"
            }
            "editar_movimiento" -> {
                val desc = args["descripcion"]?.toString()?.takeIf { it.isNotBlank() }
                    ?: args["nota"]?.toString()?.takeIf { it.isNotBlank() && !it.equals("Sin nota", ignoreCase = true) }
                    ?: "este movimiento"
                val monto = args["monto"]?.toString()?.toDoubleOrNull()?.let { "$currencySymbol${String.format(Locale.US, "%.2f", it)}" }
                val cat = args["categoria"]?.toString()?.takeIf { it.isNotBlank() && !it.contains("actual", ignoreCase = true) }
                val nota = args["nota"]?.toString()?.takeIf { it.isNotBlank() && !it.equals("Sin nota", ignoreCase = true) }
                val fecha = args["fecha"]?.toString()?.let { formatShortDate(it) }
                val detalles = listOfNotNull(
                    monto?.let { "Monto: $it" },
                    cat?.let { "Categoría: $it" },
                    nota?.let { "Nota: '$it'" },
                    fecha?.let { "Fecha: $it" }
                ).joinToString(", ")
                "¿Modifico el movimiento '$desc' ($detalles)?"
            }
            "eliminar_movimiento" -> {
                val desc = args["descripcion"]?.toString()?.takeIf { it.isNotBlank() } ?: "este movimiento"
                "¿Elimino el movimiento '$desc'? (Esta acción no se puede deshacer)"
            }
            "crear_meta" -> {
                val nombre = args["nombre"]?.toString() ?: "Nueva meta"
                val monto = args["montoObjetivo"]?.toString()?.toDoubleOrNull() ?: 0.0
                val formattedAmount = "$currencySymbol${String.format(Locale.US, "%.2f", monto)}"
                val emoji = args["emoji"]?.toString() ?: "🎯"
                val fecha = args["fechaLimite"]?.toString()?.takeIf { it.isNotBlank() }?.let { ", Fecha límite: ${formatShortDate(it)}" } ?: ""
                "¿Creo la meta de ahorro $emoji '$nombre' con objetivo de $formattedAmount$fecha?"
            }
            "editar_meta" -> {
                val nombre = args["nombre"]?.toString() ?: "meta"
                val nuevoNombre = args["nuevoNombre"]?.toString()
                val monto = args["nuevoMontoObjetivo"]?.toString()?.toDoubleOrNull()?.let { "$currencySymbol${String.format(Locale.US, "%.2f", it)}" }
                val fecha = args["nuevaFechaLimite"]?.toString()?.let { formatShortDate(it) }
                val cambios = listOfNotNull(
                    nuevoNombre?.let { "Nombre: '$it'" },
                    monto?.let { "Objetivo: $it" },
                    fecha?.let { "Fecha límite: $it" }
                ).joinToString(", ")
                "¿Modifico la meta '$nombre' ($cambios)?"
            }
            "eliminar_meta" -> {
                val nombre = args["nombre"]?.toString() ?: "Meta"
                val ahorrado = args["dineroAhorrado"]?.toString()?.toDoubleOrNull() ?: 0.0
                if (ahorrado > 0) {
                    val formattedAhorro = "$currencySymbol${String.format(Locale.US, "%.2f", ahorrado)}"
                    "¿Elimino la meta '$nombre'? Sus $formattedAhorro acumulados regresarán automáticamente a tu saldo disponible."
                } else {
                    "¿Elimino la meta de ahorro '$nombre'? (Esta acción no se puede deshacer)"
                }
            }
            "aportar_a_meta" -> {
                val monto = args["monto"]?.toString()?.toDoubleOrNull() ?: 0.0
                val formattedAmount = "$currencySymbol${String.format(Locale.US, "%.2f", monto)}"
                val metaNombre = args["metaNombre"]?.toString()?.takeIf { it.isNotBlank() } ?: "tu meta"
                "¿Aporto $formattedAmount a '$metaNombre'? (Se descuenta del saldo disponible sin contar como gasto)"
            }
            "retirar_de_meta" -> {
                val monto = args["monto"]?.toString()?.toDoubleOrNull() ?: 0.0
                val formattedAmount = "$currencySymbol${String.format(Locale.US, "%.2f", monto)}"
                val metaNombre = args["metaNombre"]?.toString()?.takeIf { it.isNotBlank() } ?: "tu meta"
                "¿Retiro $formattedAmount de '$metaNombre' de regreso a tu saldo disponible?"
            }
            "crear_categoria" -> {
                val nombre = args["nombre"]?.toString() ?: ""
                val tipo = if (args["tipo"]?.toString()?.uppercase() == "INCOME") "ingresos" else "gastos"
                val icono = args["icono"]?.toString() ?: "🏷️"
                "¿Creo la categoría $icono '$nombre' para $tipo?"
            }
            "editar_categoria" -> {
                val nuevoNombre = args["nuevoNombre"]?.toString() ?: ""
                "¿Renombro la categoría a '$nuevoNombre'?"
            }
            "eliminar_categoria" -> {
                val nombre = args["nombre"]?.toString() ?: ""
                "¿Elimino la categoría '$nombre'? (Sus movimientos no se borrarán; pasarán a General)"
            }
            "crear_lista_compras" -> {
                val nombre = args["nombre"]?.toString() ?: "Compras"
                "¿Creo la lista de compras '$nombre'?"
            }
            "agregar_producto_compra" -> {
                val prod = args["nombreProducto"]?.toString() ?: "Producto"
                val cant = args["cantidad"]?.toString() ?: "1"
                val precio = args["precioEstimado"]?.toString()?.toDoubleOrNull()
                val precioStr = if (precio != null && precio > 0) " a ~$currencySymbol${String.format(java.util.Locale.US, "%.2f", precio)}" else ""
                "¿Agrego $cant x '$prod'$precioStr a tu lista de compras?"
            }
            "editar_producto_compra" -> {
                val prod = args["nombreProducto"]?.toString() ?: "Producto"
                val cant = args["cantidad"]?.toString()
                val precio = args["precioEstimado"]?.toString()?.toDoubleOrNull()?.let { "$currencySymbol${String.format(java.util.Locale.US, "%.2f", it)}" }
                "¿Modifico el producto '$prod' (${listOfNotNull(cant?.let { "Cantidad: $it" }, precio?.let { "Precio: $it" }).joinToString(", ")})?"
            }
            "eliminar_producto_compra" -> {
                val prod = args["nombreProducto"]?.toString() ?: "Producto"
                "¿Elimino '$prod' de la lista de compras?"
            }
            "cambiar_moneda" -> {
                val nuevo = args["nuevoSimbolo"]?.toString() ?: "$"
                "¿Cambio la moneda principal de la app a '$nuevo'?"
            }
            "borrar_todos_los_movimientos" -> {
                val conCopia = args["conCopiaDeSeguridad"]?.toString()?.toBoolean() ?: true
                val total = args["totalMovimientos"]?.toString() ?: "todos los"
                if (conCopia) {
                    "¿Crear copia de seguridad en JSON y eliminar los $total movimientos registrados en la app?"
                } else {
                    "⚠️ ADVERTENCIA: ¿Eliminar los $total movimientos de la app SIN crear copia de seguridad?"
                }
            }
            else -> "¿Deseas ejecutar la acción '$name'?"
        }
    }

    private fun getFunctionDeclarations(currencySymbol: String): JSONArray {
        val arr = JSONArray()

        // 1. consultar_movimientos
        arr.put(JSONObject().apply {
            put("name", "consultar_movimientos")
            put("description", "Consulta los movimientos registrados con filtros opcionales (mes, año, categoría, tipo).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("mes", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Mes del 1 al 12")
                    })
                    put("anio", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Año (ej. 2026)")
                    })
                    put("categoria", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Nombre de la categoría")
                    })
                    put("tipo", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "EXPENSE para gastos o INCOME para ingresos")
                    })
                    put("limite", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Máximo de resultados a devolver (por defecto 20)")
                    })
                })
            })
        })

        // 2. consultar_resumen
        arr.put(JSONObject().apply {
            put("name", "consultar_resumen")
            put("description", "Consulta el balance, total de ingresos, total de gastos, ahorros y desglose de un mes.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("mes", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Mes del 1 al 12")
                    })
                    put("anio", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Año")
                    })
                })
            })
        })

        // 3. listar_categorias
        arr.put(JSONObject().apply {
            put("name", "listar_categorias")
            put("description", "Lista las categorías disponibles con sus nombres, emojis y límites.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 4. listar_metas
        arr.put(JSONObject().apply {
            put("name", "listar_metas")
            put("description", "Lista las metas de ahorro con su id, nombre, objetivo y acumulado actual.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 5. listar_compras
        arr.put(JSONObject().apply {
            put("name", "listar_compras")
            put("description", "Lista los productos y listas de compras pendientes.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 6. agregar_movimiento
        arr.put(JSONObject().apply {
            put("name", "agregar_movimiento")
            put("description", "Agrega un nuevo gasto o ingreso. Para metas de ahorro usa crear_meta o aportar_a_meta.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("tipo", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "EXPENSE (gasto) o INCOME (ingreso)")
                    })
                    put("monto", JSONObject().apply {
                        put("type", "NUMBER")
                        put("description", "Monto numérico en la moneda de la app")
                    })
                    put("categoria", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Nombre exacto o sugerido de la categoría")
                    })
                    put("nota", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Descripción o concepto del movimiento")
                    })
                    put("fecha", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Fecha en formato YYYY-MM-DD (opcional, por defecto hoy)")
                    })
                    put("hora", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Hora en formato HH:mm (opcional)")
                    })
                })
                put("required", JSONArray().apply {
                    put("tipo")
                    put("monto")
                })
            })
        })

        // 7. editar_movimiento
        arr.put(JSONObject().apply {
            put("name", "editar_movimiento")
            put("description", "Edita el monto, categoría, fecha, hora o nota de un movimiento existente por su ID.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("id", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "ID numérico del movimiento")
                    })
                    put("monto", JSONObject().apply { put("type", "NUMBER") })
                    put("categoria", JSONObject().apply { put("type", "STRING") })
                    put("nota", JSONObject().apply { put("type", "STRING") })
                    put("fecha", JSONObject().apply { put("type", "STRING") })
                    put("hora", JSONObject().apply { put("type", "STRING") })
                })
                put("required", JSONArray().apply { put("id") })
            })
        })

        // 8. eliminar_movimiento
        arr.put(JSONObject().apply {
            put("name", "eliminar_movimiento")
            put("description", "Elimina un movimiento existente por su ID.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("id", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "ID numérico del movimiento")
                    })
                    put("descripcion", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Descripción del movimiento para mostrar al usuario")
                    })
                })
                put("required", JSONArray().apply {
                    put("id")
                    put("descripcion")
                })
            })
        })

        // 9. crear_meta (Rule 2 & 4)
        arr.put(JSONObject().apply {
            put("name", "crear_meta")
            put("description", "Crea una nueva meta de ahorro con su nombre y monto objetivo. Úsala siempre que el usuario quiera una meta.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("nombre", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Nombre de la meta (ej. Vacaciones, Coche nuevo)")
                    })
                    put("montoObjetivo", JSONObject().apply {
                        put("type", "NUMBER")
                        put("description", "Monto que se desea reunir")
                    })
                    put("emoji", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Emoji para la meta (ej. 🏖️, 🚗)")
                    })
                    put("fechaLimite", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Fecha límite en formato YYYY-MM-DD (opcional)")
                    })
                })
                put("required", JSONArray().apply {
                    put("nombre")
                    put("montoObjetivo")
                })
            })
        })

        // 10. editar_meta
        arr.put(JSONObject().apply {
            put("name", "editar_meta")
            put("description", "Modifica el nombre, monto objetivo, emoji o fecha límite de una meta existente.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("id", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "ID de la meta")
                    })
                    put("nuevoNombre", JSONObject().apply { put("type", "STRING") })
                    put("nuevoMontoObjetivo", JSONObject().apply { put("type", "NUMBER") })
                    put("nuevoEmoji", JSONObject().apply { put("type", "STRING") })
                    put("nuevaFechaLimite", JSONObject().apply { put("type", "STRING") })
                })
                put("required", JSONArray().apply { put("id") })
            })
        })

        // 11. eliminar_meta
        arr.put(JSONObject().apply {
            put("name", "eliminar_meta")
            put("description", "Elimina una meta de ahorro. Si tiene dinero ahorrado, regresa al saldo disponible.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("id", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "ID de la meta a eliminar")
                    })
                    put("nombre", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Nombre de la meta")
                    })
                    put("dineroAhorrado", JSONObject().apply {
                        put("type", "NUMBER")
                        put("description", "Monto ahorrado actual en la meta que regresará al saldo (opcional)")
                    })
                })
                put("required", JSONArray().apply {
                    put("id")
                    put("nombre")
                })
            })
        })

        // 12. aportar_a_meta
        arr.put(JSONObject().apply {
            put("name", "aportar_a_meta")
            put("description", "Aporta dinero a una meta de ahorro existente. Descuenta del saldo del mes sin contar como gasto.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("metaId", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "ID de la meta")
                    })
                    put("monto", JSONObject().apply {
                        put("type", "NUMBER")
                        put("description", "Monto a aportar")
                    })
                    put("metaNombre", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Nombre de la meta para confirmación")
                    })
                    put("nota", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Nota opcional del aporte")
                    })
                })
                put("required", JSONArray().apply {
                    put("metaId")
                    put("monto")
                })
            })
        })

        // 13. retirar_de_meta
        arr.put(JSONObject().apply {
            put("name", "retirar_de_meta")
            put("description", "Retira dinero ahorrado de una meta regresándolo al saldo disponible.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("metaId", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "ID de la meta")
                    })
                    put("monto", JSONObject().apply {
                        put("type", "NUMBER")
                        put("description", "Monto a retirar")
                    })
                    put("metaNombre", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Nombre de la meta")
                    })
                    put("nota", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Motivo del retiro")
                    })
                })
                put("required", JSONArray().apply {
                    put("metaId")
                    put("monto")
                })
            })
        })

        // 14. crear_categoria
        arr.put(JSONObject().apply {
            put("name", "crear_categoria")
            put("description", "Crea una nueva categoría de gastos o ingresos.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("nombre", JSONObject().apply { put("type", "STRING") })
                    put("tipo", JSONObject().apply { put("type", "STRING") })
                    put("icono", JSONObject().apply { put("type", "STRING") })
                })
                put("required", JSONArray().apply { put("nombre") })
            })
        })

        // 15. editar_categoria
        arr.put(JSONObject().apply {
            put("name", "editar_categoria")
            put("description", "Modifica el nombre de una categoría existente.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("id", JSONObject().apply { put("type", "STRING") })
                    put("nuevoNombre", JSONObject().apply { put("type", "STRING") })
                })
                put("required", JSONArray().apply { put("id") })
            })
        })

        // 16. eliminar_categoria
        arr.put(JSONObject().apply {
            put("name", "eliminar_categoria")
            put("description", "Elimina una categoría existente.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("id", JSONObject().apply { put("type", "STRING") })
                    put("nombre", JSONObject().apply { put("type", "STRING") })
                })
                put("required", JSONArray().apply {
                    put("id")
                    put("nombre")
                })
            })
        })

        // 17. crear_lista_compras
        arr.put(JSONObject().apply {
            put("name", "crear_lista_compras")
            put("description", "Crea una nueva lista de compras.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("nombre", JSONObject().apply { put("type", "STRING") })
                })
                put("required", JSONArray().apply { put("nombre") })
            })
        })

        // 18. agregar_producto_compra
        arr.put(JSONObject().apply {
            put("name", "agregar_producto_compra")
            put("description", "Agrega un producto a la lista de compras.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("listaId", JSONObject().apply { put("type", "INTEGER") })
                    put("nombreProducto", JSONObject().apply { put("type", "STRING") })
                    put("cantidad", JSONObject().apply { put("type", "NUMBER") })
                    put("precioEstimado", JSONObject().apply { put("type", "NUMBER") })
                })
                put("required", JSONArray().apply { put("nombreProducto") })
            })
        })

        // 19. editar_producto_compra
        arr.put(JSONObject().apply {
            put("name", "editar_producto_compra")
            put("description", "Edita el nombre, cantidad, precio o estado comprado de un producto de compras.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("id", JSONObject().apply { put("type", "INTEGER") })
                    put("nombreProducto", JSONObject().apply { put("type", "STRING") })
                    put("cantidad", JSONObject().apply { put("type", "NUMBER") })
                    put("precioEstimado", JSONObject().apply { put("type", "NUMBER") })
                    put("comprado", JSONObject().apply { put("type", "BOOLEAN") })
                })
                put("required", JSONArray().apply { put("id") })
            })
        })

        // 20. eliminar_producto_compra
        arr.put(JSONObject().apply {
            put("name", "eliminar_producto_compra")
            put("description", "Elimina un producto de la lista de compras.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("id", JSONObject().apply { put("type", "INTEGER") })
                    put("nombreProducto", JSONObject().apply { put("type", "STRING") })
                })
                put("required", JSONArray().apply {
                    put("id")
                    put("nombreProducto")
                })
            })
        })

        // 21. cambiar_moneda
        arr.put(JSONObject().apply {
            put("name", "cambiar_moneda")
            put("description", "Cambia el símbolo de moneda configurado en la app.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("nuevoSimbolo", JSONObject().apply { put("type", "STRING") })
                })
                put("required", JSONArray().apply { put("nuevoSimbolo") })
            })
        })

        // 22. borrar_todos_los_movimientos (Bulk deletion with backup)
        arr.put(JSONObject().apply {
            put("name", "borrar_todos_los_movimientos")
            put("description", "Borra todos los movimientos de la app tras confirmar. Puede generar copia de seguridad previa.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("conCopiaDeSeguridad", JSONObject().apply {
                        put("type", "BOOLEAN")
                        put("description", "true para respaldar antes de borrar; false para borrar sin respaldo")
                    })
                    put("totalMovimientos", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Total exacto de movimientos a borrar")
                    })
                })
                put("required", JSONArray().apply { put("conCopiaDeSeguridad") })
            })
        })

        return arr
    }
}

object GeminiHistorySanitizer {
    /**
     * Enforces the mandatory Gemini turn sequence:
     * 1. History must start with a 'user' turn.
     * 2. Roles must strictly alternate: user -> model -> user -> model.
     * 3. Every model turn containing functionCall(s) MUST be immediately followed by a user turn
     *    with matching functionResponse(s) (same count, same names).
     * 4. Any user turn with functionResponse(s) MUST be immediately preceded by a model turn with matching functionCall(s).
     * 5. The last turn before sending to the model MUST be 'user'.
     */
    fun sanitize(rawHistory: List<GeminiMessage>): MutableList<GeminiMessage> {
        if (rawHistory.isEmpty()) return mutableListOf()

        val result = mutableListOf<GeminiMessage>()

        // 1. Must start with a user turn
        var startIndex = 0
        while (startIndex < rawHistory.size && rawHistory[startIndex].role != "user") {
            startIndex++
        }
        if (startIndex >= rawHistory.size) return mutableListOf()

        for (i in startIndex until rawHistory.size) {
            val msg = rawHistory[i]
            if (msg.parts.isEmpty()) continue

            if (result.isEmpty()) {
                if (msg.role == "user") {
                    result.add(msg)
                }
                continue
            }

            val prev = result.last()

            // Check if previous was a model turn with functionCall(s)
            val prevCalls = prev.getFunctionCallNames()
            if (prev.isModel() && prevCalls.isNotEmpty()) {
                // Next turn MUST be a user turn with functionResponse(s)
                if (msg.isUser() && msg.hasFunctionResponse()) {
                    val respNames = msg.getFunctionResponseNames()
                    if (respNames == prevCalls) {
                        result.add(msg)
                    } else {
                        // Fix mismatch: create exact matching functionResponses
                        val fixedParts = mutableListOf<JSONObject>()
                        for (callName in prevCalls) {
                            val matchedResp = msg.parts.firstOrNull {
                                it.optJSONObject("functionResponse")?.optString("name") == callName
                            }
                            if (matchedResp != null) {
                                fixedParts.add(matchedResp)
                            } else {
                                fixedParts.add(createCancelledResponse(callName))
                            }
                        }
                        result.add(GeminiMessage(role = "user", parts = fixedParts))
                    }
                } else {
                    // Current message is not a functionResponse (e.g. user typed a new text message).
                    // Insert the missing functionResponse turn for the model calls first:
                    val cancelParts = prevCalls.map { createCancelledResponse(it) }
                    result.add(GeminiMessage(role = "user", parts = cancelParts))
                    // Add synthetic model transition so the new user turn can follow:
                    result.add(GeminiMessage(role = "model", parts = listOf(JSONObject().put("text", "Entendido, acción cancelada."))))

                    if (msg.isUser()) {
                        result.add(msg)
                    }
                }
                continue
            }

            // If current message is a functionResponse, but previous was NOT a model with matching functionCall, drop orphan
            if (msg.isUser() && msg.hasFunctionResponse()) {
                continue
            }

            // Normal turn handling: strictly alternate roles
            if (msg.role == prev.role) {
                if (msg.role == "user") {
                    val mergedParts = prev.parts + msg.parts
                    result[result.size - 1] = GeminiMessage(role = "user", parts = mergedParts)
                } else {
                    result[result.size - 1] = msg
                }
            } else {
                result.add(msg)
            }
        }

        // Final check: if the very last turn is a model with functionCalls, provide cancellation responses
        if (result.isNotEmpty() && result.last().isModel()) {
            val lastCalls = result.last().getFunctionCallNames()
            if (lastCalls.isNotEmpty()) {
                val cancelParts = lastCalls.map { createCancelledResponse(it) }
                result.add(GeminiMessage(role = "user", parts = cancelParts))
            }
        }

        return result
    }

    fun createCancelledResponse(toolName: String, status: String = "cancelado_por_el_usuario"): JSONObject {
        return JSONObject().apply {
            put("functionResponse", JSONObject().apply {
                put("name", toolName)
                put("response", JSONObject().apply {
                    put("status", status)
                    put("mensaje", "La acción fue cancelada.")
                })
            })
        }
    }
}
