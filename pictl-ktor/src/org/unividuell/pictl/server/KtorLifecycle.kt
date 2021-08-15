package org.unividuell.pictl.server

import io.ktor.application.*
import org.koin.ktor.ext.inject
import org.unividuell.pictl.server.usecase.SubscribeForPlayersUpdatesInteractor

fun Application.lifecycleMonitor() {
    environment.monitor.subscribe(ApplicationStopped) {
        val subscribeForPlayersUpdatesInteractor: SubscribeForPlayersUpdatesInteractor by inject()
        subscribeForPlayersUpdatesInteractor.bye()
    }
}