package org.unividuell.pictl.server.repository.cometd.model

data class SubscribeCometRequest(
    val response: String,
    val request: List<Any>
)