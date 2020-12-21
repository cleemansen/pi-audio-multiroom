package org.unividuell.pictl.server.repository.cometd.model

data class SlimCometRequest(
    val response: String,
    val request: List<Any>
)