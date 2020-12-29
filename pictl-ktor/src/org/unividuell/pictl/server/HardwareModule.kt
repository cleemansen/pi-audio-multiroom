package org.unividuell.pictl.server

import io.ktor.application.*
import io.ktor.routing.*
import org.kodein.di.ktor.di
import org.unividuell.pictl.server.controller.hardwareRoutes

fun Application.hardwareModule(testing: Boolean = false) {

    routing {
        hardwareRoutes(di())
    }
}