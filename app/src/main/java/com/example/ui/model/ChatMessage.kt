package com.example.ui.model

import com.example.data.service.GeminiFunctionCallItem

enum class MessageSender {
    USER,
    ASSISTANT
}

enum class ActionStatus {
    PENDING,
    EXECUTING,
    CONFIRMED,
    CANCELLED
}

data class ChatActionProposal(
    val id: String,
    val toolName: String,
    val summary: String,
    val isDestructive: Boolean,
    val arguments: Map<String, Any?>,
    val status: ActionStatus = ActionStatus.PENDING,
    val resultText: String? = null,
    val allCalls: List<GeminiFunctionCallItem> = emptyList()
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String = "",
    val actionProposal: ChatActionProposal? = null,
    val timestamp: Long = System.currentTimeMillis()
)
