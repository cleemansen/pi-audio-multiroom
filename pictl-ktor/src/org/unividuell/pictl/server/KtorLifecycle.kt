package org.unividuell.pictl.server

import io.ktor.application.*
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.unividuell.pictl.server.repository.SqueezeboxCometLongPollingRepository

fun Application.lifecycleMonitor() {
    environment.monitor.subscribe(ApplicationStopped) {
        val cometd: SqueezeboxCometLongPollingRepository by di().instance()
        cometd.bye()
    }
}