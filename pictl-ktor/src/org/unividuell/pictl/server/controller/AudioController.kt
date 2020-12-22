package org.unividuell.pictl.server.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.unividuell.pictl.server.repository.SqueezeboxCometSubscriptionRepository
import org.unividuell.pictl.server.usecase.*
import java.util.*
import kotlin.collections.LinkedHashSet

fun Route.audioRoutes() {

    val getCurrentSongInteractor: GetCurrentSongInteractor by di().instance()
    val subscribeForPlayersUpdatesInteractor: SubscribeForPlayersUpdatesInteractor by di().instance()
    val requestPlayersUpdatesInteractor by di().instance<RequestPlayersUpdatesInteractor>()
    val togglePlayPausePlayerInteractor: TogglePlayPausePlayerInteractor by di().instance()
    val changeVolumeInteractor by di().instance<ChangeVolumeInteractor>()

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
                subscribeForPlayersUpdatesInteractor.start()
            } else {
                // request update from server for all connections (including this new connection)
                requestPlayersUpdatesInteractor.requestUpdate()
            }
            audioWsConnections += this
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
                    subscribeForPlayersUpdatesInteractor.stop()
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
