package org.unividuell.pictl.server.usecase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RequestPlayersUpdatesInteractor : KoinComponent {

    private val dataSource: DataSource by inject()

    interface DataSource {
        fun requestUpdate()
    }

    fun requestUpdate() {
        dataSource.requestUpdate()
    }

}