package org.unividuell.pictl.server.usecase

import org.kodein.di.DI
import org.kodein.di.instance

class SubscribeForPlayersUpdatesInteractor(di: DI) {

    private val dataSource: DataSource by di.instance()

    interface DataSource {
        fun connectAndSubscribe()
        fun unsubscribe()
        fun disconnect()
    }

    fun start() {
        dataSource.connectAndSubscribe()
    }

    fun stop() {
        dataSource.unsubscribe()
    }

    fun bye() {
        dataSource.disconnect()
    }

}