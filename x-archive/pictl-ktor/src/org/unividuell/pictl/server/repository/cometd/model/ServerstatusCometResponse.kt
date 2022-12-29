package org.unividuell.pictl.server.repository.cometd.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ServerstatusCometResponse(
    val players: List<Player>
) {
    data class Player(
        val power: Int,
        val connected: Int,
        val name: String,
        @JsonProperty("playerid")
        val playerId: String
    )
}