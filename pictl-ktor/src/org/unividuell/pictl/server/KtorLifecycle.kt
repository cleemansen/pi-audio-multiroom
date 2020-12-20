package org.unividuell.pictl.server

import io.ktor.application.*
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.unividuell.pictl.server.usecase.SubscribeForPlayersUpdatesInteractor

fun Application.lifecycleMonitor() {
    environment.monitor.subscribe(ApplicationStopped) {
        val subscribeForPlayersUpdatesInteractor: SubscribeForPlayersUpdatesInteractor by di().instance()
        subscribeForPlayersUpdatesInteractor.bye()
    }
}