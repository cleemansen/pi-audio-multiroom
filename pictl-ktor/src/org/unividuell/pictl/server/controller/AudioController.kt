package org.unividuell.pictl.server.controller

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.unividuell.pictl.server.usecase.GetCurrentSongInteractor

fun Route.audioRoutes() {

    val getCurrentSongInteractor: GetCurrentSongInteractor by di().instance()

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
            send(Frame.Text("Hi from server"))
            while (true) {
                val frame = incoming.receive()
                if (frame is Frame.Text) {
                    send(Frame.Text("Client said: " + frame.readText()))
                }
            }
        }

        webSocket("/echo") {
            try {
                while (true) {
                    val text = (incoming.receive() as Frame.Text).readText()
                    outgoing.send(Frame.Text(text))
                }
            } catch (e: ClosedReceiveChannelException) {
                // do nothing!
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}