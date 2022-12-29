package org.unividuell.pictl.server

import io.ktor.application.*
import org.koin.ktor.ext.KoinApplicationStopPreparing
import org.koin.ktor.ext.inject
import org.unividuell.pictl.server.usecase.SubscribeForPlayersUpdatesInteractor

fun Application.lifecycleMonitor() {
    val subscribeForPlayersUpdatesInteractor: SubscribeForPlayersUpdatesInteractor by inject()

    environment.monitor.subscribe(KoinApplicationStopPreparing) {
        subscribeForPlayersUpdatesInteractor.bye()
    }
}