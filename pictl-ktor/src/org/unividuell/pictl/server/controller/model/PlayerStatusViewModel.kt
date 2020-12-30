package org.unividuell.pictl.server.controller.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.InetSocketAddress

data class PlayerStatusViewModel(
    val playerId: String,
    val playerName: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val remoteTitle: String? = null,
    val artworkUrl: String? = null,
    val mode: String? = null,
    val mixerVolume: Int? = null,
    val connected: Boolean? = null,
    val ipAddress: InetSocketAddress? = null,
    val syncController: String? = null,
    val syncNodes: List<String> = emptyList()
) {
    fun toJson() = jacksonObjectMapper().writeValueAsString(this)
}