package org.unividuell.pictl.server.usecase

import org.kodein.di.DI
import org.kodein.di.instance

class SubscribeForPlayersUpdatesInteractor(di: DI) {

    private val dataSource: DataSource by di.instance()

    interface DataSource {
        fun connectAndSubscribe()
        fun requestUpdate()
        fun unsubscribe()
        fun disconnect()
    }

    fun start() {
        dataSource.connectAndSubscribe()
    }

    fun requestUpdate() {
        dataSource.requestUpdate()
    }

    fun stop() {
        dataSource.unsubscribe()
    }

    fun bye() {
        dataSource.disconnect()
    }

}