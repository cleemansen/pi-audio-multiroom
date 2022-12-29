package org.unividuell.pictl.server.usecase

import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class SubscribeForPlayersUpdatesInteractor : KoinComponent {

    private val dataSource: DataSource by inject()
    private val requestPlayersUpdatesInteractor by inject<RequestPlayersUpdatesInteractor>()

    private var stopSubscriptionJob: Job? = null
    private var connected = false

    interface DataSource {
        fun connectAndSubscribe()
        fun unsubscribe()
        fun disconnect()
    }

    fun start() {
        if (connected && stopSubscriptionJob?.isActive == true) {
            // use-case browser refresh
            stopSubscriptionJob?.cancel(message = "got new subscription request")
            requestPlayersUpdatesInteractor.requestUpdate()
        } else {
            dataSource.connectAndSubscribe()
            connected = true
        }
    }

    fun stop() {
        stopSubscriptionJob = GlobalScope.launch {
            delay(timeMillis = TimeUnit.SECONDS.toMillis(30))
            dataSource.unsubscribe()
            stopSubscriptionJob = null
            connected = false
        }
    }

    fun bye() {
        dataSource.disconnect()
        connected = false
    }

}