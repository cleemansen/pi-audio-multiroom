package org.unividuell.pictl.server.repository.cometd.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PlayerCometdResponse(
    @JsonProperty("player_name")
    val playerName: String? = null,
    @JsonProperty("sync_master")
    val syncMaster: String? = null,
    @JsonProperty("sync_slaves")
    val syncSlaves: String? = null,
    @JsonProperty("current_title")
    val currentTitle: String? = null,
    val remoteMeta: RemoteMeta? = null,
    val mode: String? = null
) {
    data class RemoteMeta(
        val title: String? = null,
        val artist: String? = null,
        @JsonProperty("remote_title")
        val remoteTitle: String? = null,
        // GET /plugins/AppGallery/html/images/icon.png HTTP/1.1
        @JsonProperty("artwork_url")
        val artworkUrl: String? = null,
        val bitrate: String? = null
    )
}