package org.unividuell.pictl.server.controller

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.kodein.di.DI
import org.kodein.di.instance

fun Routing.metricRoutes(
    di: DI
) {

    val registry: PrometheusMeterRegistry by di.instance()

    get("/metrics") {
        call.respondText {
            registry.scrape()
        }
    }
}