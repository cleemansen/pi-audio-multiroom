package org.unividuell.pictl.server.usecase

import org.kodein.di.DI
import org.kodein.di.instance

class RequestPlayersUpdatesInteractor(di: DI) {

    private val dataSource: DataSource by di.instance()

    interface DataSource {
        fun requestUpdate()
    }

    fun requestUpdate() {
        dataSource.requestUpdate()
    }

}