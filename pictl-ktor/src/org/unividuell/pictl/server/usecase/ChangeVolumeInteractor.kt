package org.unividuell.pictl.server.usecase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ChangeVolumeInteractor : KoinComponent {

    private val dataSource by inject<DataSource>()
    private val requestPlayersUpdatesInteractor by inject<RequestPlayersUpdatesInteractor>()

    interface DataSource {
        fun volumeStepUp(playerId: String)
        fun volumeStepDown(playerId: String)
        fun volumeChange(playerId: String, desiredVolume: Int)
    }

    fun volumeStepUp(playerId: String) {
        dataSource.volumeStepUp(playerId = playerId)
        requestPlayersUpdatesInteractor.requestUpdate()
    }

    fun volumeStepDown(playerId: String) {
        dataSource.volumeStepDown(playerId = playerId)
        requestPlayersUpdatesInteractor.requestUpdate()
    }

    fun volumeChange(playerId: String, desiredVolume: Int) {
        dataSource.volumeChange(playerId = playerId, desiredVolume = desiredVolume)
        requestPlayersUpdatesInteractor.requestUpdate()
    }
}