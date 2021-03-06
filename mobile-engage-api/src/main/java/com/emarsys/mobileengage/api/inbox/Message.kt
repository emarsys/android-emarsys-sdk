package com.emarsys.mobileengage.api.inbox

data class Message(
        val id: String,
        val campaignId: String,
        val collapseId: String?,
        val title: String,
        val body: String,
        val imageUrl: String?,
        val receivedAt: Long,
        val updatedAt: Long?,
        val expiresAt: Long?,
        val tags: List<String>?,
        val properties: Map<String, String>?
)