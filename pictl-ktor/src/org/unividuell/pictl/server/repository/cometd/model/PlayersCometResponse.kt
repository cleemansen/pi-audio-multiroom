package org.unividuell.pictl.server.repository.cometd.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PlayersCometResponse(
    val players: List<Player>
) {
    data class Player(
        val power: Int,
        val name: String,
        @JsonProperty("playerid")
        val playerId: String
    )
}