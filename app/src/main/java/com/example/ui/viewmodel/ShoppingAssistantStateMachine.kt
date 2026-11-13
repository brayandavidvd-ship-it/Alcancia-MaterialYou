package com.example.ui.viewmodel

import com.example.data.service.GeminiFunctionCallItem
import com.example.data.service.GeminiService

data class PendingShoppingItem(
    val name: String,
    val quantity: Double = 1.0,
    val listaId: Long? = null,
    val unitPrice: Double? = null
)

sealed class ShoppingPriceState {
    data object Idle : ShoppingPriceState()

    data class AwaitingPrices(
        val items: List<PendingShoppingItem>,
        val createdListName: String? = null
    ) : ShoppingPriceState()
}

data class MissingPriceInterception(
    val awaitingState: ShoppingPriceState.AwaitingPrices,
    val questionText: String
)

data class ResolvedShoppingBatch(
    val calls: List<GeminiFunctionCallItem>,
    val summary: String,
    val confirmationMessageText: String
)

object ShoppingAssistantStateMachine {

    private val NON_PRICE_INTENT_KEYWORDS = listOf(
        "cancela", "cancelar", "olvida", "olvídalo", "olvidalo", "mejor no",
        "resumen", "gasto", "ingreso", "meta", "borra", "elimina", "quita",
        "cuánto gasté", "cuanto gaste", "ayuda", "saldo", "moneda"
    )

    /**
     * Inspects a batch of Gemini function calls. If any `agregar_producto_compra` lacks an explicit
     * positive price (`precioEstimado == null || precioEstimado <= 0.0`), intercepts the batch BEFORE
     * showing a confirmation card and produces an awaiting-prices state + assistant question.
     */
    fun inspectShoppingActionCalls(calls: List<GeminiFunctionCallItem>): MissingPriceInterception? {
        val actionCalls = calls.filter { !it.isQuery }
        val addCalls = actionCalls.filter { it.name == "agregar_producto_compra" }
        if (addCalls.isEmpty()) return null

        val hasMissingPrice = addCalls.any { call ->
            val price = call.arguments["precioEstimado"]?.toString()?.toDoubleOrNull()
            price == null || price <= 0.0 || price.isNaN()
        }
        if (!hasMissingPrice) return null

        // Only intercept pure shopping batches (optionally preceded by crear_lista_compras)
        val hasUnsupportedNonShoppingActions = actionCalls.any {
            it.name != "agregar_producto_compra" && it.name != "crear_lista_compras"
        }
        if (hasUnsupportedNonShoppingActions) return null

        val createdListName = actionCalls
            .firstOrNull { it.name == "crear_lista_compras" }
            ?.arguments?.get("nombre")?.toString()?.trim()?.takeIf { it.isNotBlank() }

        val pendingItems = addCalls.map { call ->
            val name = call.arguments["nombreProducto"]?.toString()?.trim()?.ifBlank { "este producto" } ?: "este producto"
            val qty = call.arguments["cantidad"]?.toString()?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
            val listaId = call.arguments["listaId"]?.toString()?.toLongOrNull()?.takeIf { it > 0L }
            val price = call.arguments["precioEstimado"]?.toString()?.toDoubleOrNull()?.takeIf { it > 0.0 && !it.isNaN() }
            PendingShoppingItem(
                name = name,
                quantity = qty,
                listaId = listaId,
                unitPrice = price
            )
        }

        val missingItems = pendingItems.filter { it.unitPrice == null || it.unitPrice <= 0.0 }
        val questionText = if (missingItems.size == 1) {
            "¿Cuánto cuesta ${missingItems.first().name}?"
        } else {
            val numbered = missingItems.mapIndexed { idx, item -> "${idx + 1}. ${item.name}" }.joinToString("\n")
            "Para agregar estos productos con su presupuesto, dime cuánto cuesta cada uno:\n$numbered"
        }

        return MissingPriceInterception(
            awaitingState = ShoppingPriceState.AwaitingPrices(
                items = pendingItems,
                createdListName = createdListName
            ),
            questionText = questionText
        )
    }

    /**
     * Attempts to bind numeric price(s) from [userReply] to the pending items missing prices
     * in exact order of appearance. Returns null if the user reply is not a matching price reply
     * (e.g., user changed topic, cancelled, or gave a different count of numbers).
     */
    fun tryResolvePrices(
        state: ShoppingPriceState.AwaitingPrices,
        userReply: String,
        currencySymbol: String
    ): ResolvedShoppingBatch? {
        val trimmed = userReply.trim()
        if (trimmed.isEmpty() || trimmed.contains("?")) return null

        val lower = trimmed.lowercase()
        if (NON_PRICE_INTENT_KEYWORDS.any { lower.contains(it) }) {
            return null
        }

        val missingIndices = state.items.indices.filter { idx ->
            val p = state.items[idx].unitPrice
            p == null || p <= 0.0
        }
        if (missingIndices.isEmpty()) return null

        val extractedPrices = extractPrices(trimmed, missingIndices.size) ?: return null

        val updatedItems = state.items.toMutableList()
        missingIndices.forEachIndexed { pos, itemIndex ->
            val old = updatedItems[itemIndex]
            updatedItems[itemIndex] = old.copy(unitPrice = extractedPrices[pos])
        }

        val calls = mutableListOf<GeminiFunctionCallItem>()
        if (!state.createdListName.isNullOrBlank()) {
            calls.add(
                GeminiFunctionCallItem(
                    name = "crear_lista_compras",
                    arguments = mapOf("nombre" to state.createdListName),
                    isQuery = false
                )
            )
        }

        for (item in updatedItems) {
            val args = mutableMapOf<String, Any?>(
                "nombreProducto" to item.name,
                "cantidad" to item.quantity,
                "precioEstimado" to (item.unitPrice ?: 0.0)
            )
            if (item.listaId != null) {
                args["listaId"] = item.listaId
            }
            calls.add(
                GeminiFunctionCallItem(
                    name = "agregar_producto_compra",
                    arguments = args,
                    isQuery = false
                )
            )
        }

        val summary = GeminiService.buildBatchActionSummary(calls, currencySymbol)
        val confirmLines = updatedItems.map { item ->
            val qtyStr = GeminiService.formatQuantity(item.quantity)
            val priceStr = GeminiService.formatExactCurrency(item.unitPrice ?: 0.0, currencySymbol)
            "• $qtyStr x ${item.name} — $priceStr"
        }
        val confirmMsg = if (updatedItems.size == 1) {
            val single = updatedItems.first()
            val priceStr = GeminiService.formatExactCurrency(single.unitPrice ?: 0.0, currencySymbol)
            "Listo, agregué ${GeminiService.formatQuantity(single.quantity)} x ${single.name} ($priceStr) a tu lista de compras."
        } else {
            "Listo, agregué los ${updatedItems.size} productos a tu lista de compras:\n" + confirmLines.joinToString("\n")
        }

        return ResolvedShoppingBatch(
            calls = calls,
            summary = summary,
            confirmationMessageText = confirmMsg
        )
    }

    internal fun extractPrices(text: String, expectedCount: Int): List<Double>? {
        if (expectedCount <= 0) return null

        val primaryRegex = if (expectedCount == 1) {
            Regex("""(?<!\d)(\d+(?:[.,]\d{1,2})?)(?!\d)""")
        } else {
            Regex("""(?<!\d)(\d+(?:\.\d{1,2})?)(?!\d)""")
        }

        var matches = primaryRegex.findAll(text)
            .mapNotNull { it.groupValues[1].replace(',', '.').toDoubleOrNull() }
            .filter { it > 0.0 && !it.isNaN() && !it.isInfinite() }
            .toList()

        // Fallback for multiple items formatted with decimal commas, e.g. "25,50 y 30,00"
        if (matches.size != expectedCount && expectedCount > 1) {
            val commaDecimalMatches = Regex("""(?<!\d)(\d+,\d{1,2})(?!\d)""")
                .findAll(text)
                .mapNotNull { it.groupValues[1].replace(',', '.').toDoubleOrNull() }
                .filter { it > 0.0 && !it.isNaN() && !it.isInfinite() }
                .toList()
            if (commaDecimalMatches.size == expectedCount) {
                matches = commaDecimalMatches
            }
        }

        return if (matches.size == expectedCount) matches else null
    }
}
