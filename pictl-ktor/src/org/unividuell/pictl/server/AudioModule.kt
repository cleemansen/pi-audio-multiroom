package org.unividuell.pictl.server

import io.ktor.application.*
import io.ktor.routing.*
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.unividuell.pictl.server.controller.audioRoutes
import org.unividuell.pictl.server.repository.SlimboxCometLongPollingRepository

fun Application.audioModule() {
    routing {
        audioRoutes()
        val cometD: SlimboxCometLongPollingRepository by di().instance()
        cometD.play()
    }
}