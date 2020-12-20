package org.unividuell.pictl.server.repository.cometd.model

data class SlimSubscribeCometRequest(
    val response: String,
    val request: List<Any>
)