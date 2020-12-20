package org.unividuell.pictl.server

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.unividuell.pictl.server.controller.audioRoutes
import org.unividuell.pictl.server.repository.SqueezeboxCometLongPollingRepository
import java.time.Duration

fun Application.audioModule(testing: Boolean = false) {

    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        audioRoutes()
        val cometD: SqueezeboxCometLongPollingRepository by di().instance()
        cometD.play()
    }
}