package org.unividuell.pictl.server.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.ktor.ext.inject
import org.unividuell.pictl.server.repository.SqueezeboxCometSubscriptionRepository
import org.unividuell.pictl.server.usecase.*
import java.util.*

fun Route.audioRoutes() {

    val logger = KotlinLogging.logger { }

    val getCurrentSongInteractor: GetCurrentSongInteractor by inject()
    val subscribeForPlayersUpdatesInteractor: SubscribeForPlayersUpdatesInteractor by inject()
    val requestPlayersUpdatesInteractor by inject<RequestPlayersUpdatesInteractor>()
    val togglePlayPausePlayerInteractor: TogglePlayPausePlayerInteractor by inject()
    val changeVolumeInteractor by inject<ChangeVolumeInteractor>()

    val audioWsConnections = Collections.synchronizedSet(LinkedHashSet<DefaultWebSocketSession>())

    route("/ctl-audio") {
        get("/current") {
            val song = getCurrentSongInteractor.getCurrentSong()
            if (song != null) {
                call.respond(status = HttpStatusCode.OK, message = song)
            } else {
                call.respond(status = HttpStatusCode.NotFound, message = "")
            }
        }

        webSocket("/ws") {
            if (audioWsConnections.isEmpty()) {
                audioWsConnections += this
                logger.info { "the first ws-connection, starting cometd-subscription. added '${this.call.request.origin.remoteHost}'." }
                subscribeForPlayersUpdatesInteractor.start()
            } else {
                audioWsConnections += this
                logger.info { "next ws-connection, requesting cometd-update. Now ${audioWsConnections.size} connection(s). added '${this.call.request.origin.remoteHost}'." }
                // request update from server for all connections (including this new connection)
                requestPlayersUpdatesInteractor.requestUpdate()
            }
            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            application.log.info("<-- ws: ${frame.readText()}")
                            val msg = jacksonObjectMapper().readValue<Map<String, Any>>(frame.readText())
                            if (msg.containsKey("type")) {
                                when (msg["type"]) {
                                    "cmd" -> {
                                        val playerId = msg["playerId"] as String

                                        when (msg["cmd"]) {
                                            "TOGGLE_PLAY_PAUSE" -> {
                                                togglePlayPausePlayerInteractor.toggle(playerId = playerId)
                                            }
                                            "VOLUME_STEP_UP" -> {
                                                changeVolumeInteractor.volumeStepUp(playerId = playerId)
                                            }
                                            "VOLUME_STEP_DOWN" -> {
                                                changeVolumeInteractor.volumeStepDown(playerId = playerId)
                                            }
                                            "VOLUME_CHANGE" -> {
                                                changeVolumeInteractor.volumeChange(
                                                    playerId = playerId,
                                                    desiredVolume = msg["desiredVolume"] as Int
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                audioWsConnections -= this
                if (audioWsConnections.isEmpty()) {
                    logger.info { "the last ws-connection, stopping cometd-subscription. removed '${this.call.request.origin.remoteHost}'." }
                    subscribeForPlayersUpdatesInteractor.stop()
                } else {
                    logger.info { "removing ws-connection '${this.call.request.origin.remoteHost}'." }
                }
            }
        }
    }

    application.environment.monitor.subscribe(SqueezeboxCometSubscriptionRepository.PlayerEvent) { player ->
        GlobalScope.launch {
            audioWsConnections.forEach {
                it.send(player.toJson())
            }
        }
    }

}
