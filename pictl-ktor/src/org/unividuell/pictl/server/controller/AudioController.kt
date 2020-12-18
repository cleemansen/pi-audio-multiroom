package org.unividuell.pictl.server.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.unividuell.pictl.server.repository.SlimboxCometLongPollingRepository
import org.unividuell.pictl.server.usecase.GetCurrentSongInteractor
import java.util.*
import kotlin.collections.LinkedHashSet

fun Route.audioRoutes() {

    val getCurrentSongInteractor: GetCurrentSongInteractor by di().instance()

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
            audioWsConnections += this
            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            application.log.info("<-- ws: ${frame.readText()}")
                        }
                    }
                }
            } finally {
                audioWsConnections -= this
            }

        }

        webSocket("/echo") {
            try {
                send(Frame.Text("Hi from server"))
                while (true) {
                    val frame = incoming.receive()
                    if (frame is Frame.Text) {
                        send(Frame.Text("Client said: " + frame.readText()))
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                // do nothing!
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    application.environment.monitor.subscribe(SlimboxCometLongPollingRepository.PlayerEvent) { player ->
        GlobalScope.launch {
            audioWsConnections.forEach {
                it.send(player.toJson())
            }
        }
    }

}

data class PlayerStatusViewModel(
    val playerId: String,
    val playerName: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val remoteTitle: String? = null,
    val artworkUrl: String? = null,
    val syncMaster: String? = null,
    val syncSlaves: String? = null
) {
    fun toJson() = jacksonObjectMapper().writeValueAsString(this)
}
