package org.unividuell.pictl.server.controller

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.koin.ktor.ext.inject

fun Routing.metricRoutes() {

    val registry: PrometheusMeterRegistry by inject()

    get("/metrics") {
        call.respondText {
            registry.scrape()
        }
    }
}