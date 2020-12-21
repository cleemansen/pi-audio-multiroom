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
import org.unividuell.pictl.server.repository.SqueezeboxCometLongPollingRepository
import org.unividuell.pictl.server.usecase.GetCurrentSongInteractor
import org.unividuell.pictl.server.usecase.SubscribeForPlayersUpdatesInteractor
import org.unividuell.pictl.server.usecase.TogglePlayPausePlayerInteractor
import java.util.*
import kotlin.collections.LinkedHashSet

fun Route.audioRoutes() {

    val getCurrentSongInteractor: GetCurrentSongInteractor by di().instance()
    val subscribeForPlayersUpdatesInteractor: SubscribeForPlayersUpdatesInteractor by di().instance()
    val togglePlayPausePlayerInteractor: TogglePlayPausePlayerInteractor by di().instance()

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
                subscribeForPlayersUpdatesInteractor.requestUpdate()
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
                                        when (msg["cmd"]) {
                                            "TOGGLE_PLAY_PAUSE" -> {
                                                togglePlayPausePlayerInteractor.toggle(playerId = msg["playerId"] as String)
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

    application.environment.monitor.subscribe(SqueezeboxCometLongPollingRepository.PlayerEvent) { player ->
        GlobalScope.launch {
            audioWsConnections.forEach {
                it.send(player.toJson())
            }
        }
    }

}
