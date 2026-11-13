package com.example

import com.example.data.service.GeminiFunctionCallItem
import com.example.data.service.GeminiService
import com.example.data.service.GeminiTurnResult
import com.example.ui.viewmodel.ShoppingAssistantStateMachine
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShoppingAssistantLogicTest {

    @Test
    fun `parseResponse preserves all N shopping items in exact order without tilde approximation`() {
        val items = listOf(
            Triple("leche", 1, 25.0),
            Triple("huevos", 1, 45.0),
            Triple("pan", 2, 30.0),
            Triple("café", 1, 80.5)
        )

        val partsArr = JSONArray().apply {
            put(JSONObject().put("text", "Preparando los 4 productos en orden."))
            for ((name, qty, price) in items) {
                put(JSONObject().apply {
                    put("functionCall", JSONObject().apply {
                        put("name", "agregar_producto_compra")
                        put("args", JSONObject().apply {
                            put("nombreProducto", name)
                            put("cantidad", qty)
                            put("precioEstimado", price)
                        })
                    })
                })
            }
        }
        val responseJson = JSONObject().apply {
            put("candidates", JSONArray().apply {
                put(JSONObject().apply {
                    put("content", JSONObject().apply {
                        put("parts", partsArr)
                    })
                })
            })
        }.toString()

        val result = GeminiService.parseResponse(responseJson, "$")
        assertTrue(result is GeminiTurnResult.ActionProposalCall)
        val proposal = result as GeminiTurnResult.ActionProposalCall

        assertEquals(4, proposal.allCalls.size)
        assertEquals(listOf("leche", "huevos", "pan", "café"), proposal.allCalls.map { it.arguments["nombreProducto"] })
        assertEquals("Preparando los 4 productos en orden.", proposal.assistantText)
        assertFalse(proposal.summary.contains("~"))
        assertTrue(proposal.summary.contains("1. 1 x leche — $25.00"))
        assertTrue(proposal.summary.contains("2. 1 x huevos — $45.00"))
        assertTrue(proposal.summary.contains("3. 2 x pan — $30.00"))
        assertTrue(proposal.summary.contains("4. 1 x café — $80.50"))
    }

    @Test
    fun `missing price interception blocks confirmation card and asks for price`() {
        val calls = listOf(
            GeminiFunctionCallItem(
                name = "agregar_producto_compra",
                arguments = mapOf("nombreProducto" to "leche", "cantidad" to 1),
                isQuery = false
            )
        )

        val interception = ShoppingAssistantStateMachine.inspectShoppingActionCalls(calls)
        assertNotNull(interception)
        assertEquals("¿Cuánto cuesta leche?", interception!!.questionText)
        assertEquals(1, interception.awaitingState.items.size)
        assertNull(interception.awaitingState.items.first().unitPrice)
    }

    @Test
    fun `partial missing prices preserves known price and asks only missing items in order`() {
        val calls = listOf(
            GeminiFunctionCallItem(
                name = "agregar_producto_compra",
                arguments = mapOf("nombreProducto" to "leche", "cantidad" to 1, "precioEstimado" to 0),
                isQuery = false
            ),
            GeminiFunctionCallItem(
                name = "agregar_producto_compra",
                arguments = mapOf("nombreProducto" to "huevos", "cantidad" to 1, "precioEstimado" to 45),
                isQuery = false
            ),
            GeminiFunctionCallItem(
                name = "agregar_producto_compra",
                arguments = mapOf("nombreProducto" to "pan", "cantidad" to 2),
                isQuery = false
            )
        )

        val interception = ShoppingAssistantStateMachine.inspectShoppingActionCalls(calls)
        assertNotNull(interception)
        assertTrue(interception!!.questionText.contains("1. leche"))
        assertTrue(interception.questionText.contains("2. pan"))
        assertFalse(interception.questionText.contains("huevos"))

        val resolved = ShoppingAssistantStateMachine.tryResolvePrices(
            state = interception.awaitingState,
            userReply = "25 y 30 pesos",
            currencySymbol = "$"
        )
        assertNotNull(resolved)
        assertEquals(3, resolved!!.calls.size)
        assertEquals(25.0, resolved.calls[0].arguments["precioEstimado"])
        assertEquals(45.0, resolved.calls[1].arguments["precioEstimado"])
        assertEquals(30.0, resolved.calls[2].arguments["precioEstimado"])
        assertFalse(resolved.summary.contains("~"))
        assertTrue(resolved.summary.contains("1. 1 x leche — $25.00"))
        assertTrue(resolved.summary.contains("2. 1 x huevos — $45.00"))
        assertTrue(resolved.summary.contains("3. 2 x pan — $30.00"))
    }

    @Test
    fun `all items with explicit prices skip interception immediately`() {
        val calls = listOf(
            GeminiFunctionCallItem(
                name = "agregar_producto_compra",
                arguments = mapOf("nombreProducto" to "leche", "cantidad" to 1, "precioEstimado" to 25),
                isQuery = false
            )
        )

        val interception = ShoppingAssistantStateMachine.inspectShoppingActionCalls(calls)
        assertNull(interception)
    }

    @Test
    fun `topic switch or cancellation returns null from price state machine`() {
        val calls = listOf(
            GeminiFunctionCallItem(
                name = "agregar_producto_compra",
                arguments = mapOf("nombreProducto" to "leche"),
                isQuery = false
            )
        )
        val interception = ShoppingAssistantStateMachine.inspectShoppingActionCalls(calls)!!
        assertNull(ShoppingAssistantStateMachine.tryResolvePrices(interception.awaitingState, "mejor dime mi resumen", "$"))
        assertNull(ShoppingAssistantStateMachine.tryResolvePrices(interception.awaitingState, "cancelar", "$"))
    }

    @Test
    fun `editar_producto_compra summary displays real product name instead of generic Producto`() {
        val responseJson = JSONObject().apply {
            put("candidates", JSONArray().apply {
                put(JSONObject().apply {
                    put("content", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("functionCall", JSONObject().apply {
                                    put("name", "editar_producto_compra")
                                    put("args", JSONObject().apply {
                                        put("id", 7)
                                        put("nombreProducto", "Leche")
                                        put("cantidad", 3)
                                    })
                                })
                            })
                        })
                    })
                })
            })
        }.toString()

        val result = GeminiService.parseResponse(responseJson, "$")
        assertTrue(result is GeminiTurnResult.ActionProposalCall)
        val proposal = result as GeminiTurnResult.ActionProposalCall
        assertTrue(proposal.summary.contains("'Leche'"))
        assertTrue(proposal.summary.contains("Cantidad: 3"))
        assertFalse(proposal.summary.contains("'Producto'"))
    }

    @Test
    fun `editar_producto_compra resolves productName alias and multi-item edits cleanly`() {
        val calls = listOf(
            GeminiFunctionCallItem(
                name = "editar_producto_compra",
                arguments = mapOf("id" to 1, "productName" to "Pan", "cantidad" to 2),
                isQuery = false
            ),
            GeminiFunctionCallItem(
                name = "editar_producto_compra",
                arguments = mapOf("id" to 2, "nombreProducto" to "Huevos", "precioEstimado" to 48.0),
                isQuery = false
            )
        )

        val summary = GeminiService.buildBatchActionSummary(calls, "$")
        assertTrue(summary.contains("Editar 'Pan' (Cant: 2)"))
        assertTrue(summary.contains("Editar 'Huevos' ($48.00)"))
        assertFalse(summary.contains("'Producto'"))
    }
}

