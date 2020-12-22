package org.unividuell.pictl.server.usecase

import org.kodein.di.DI
import org.kodein.di.instance

class ChangeVolumeInteractor(di: DI) {

    private val dataSource by di.instance<DataSource>()
    private val requestPlayersUpdatesInteractor by di.instance<RequestPlayersUpdatesInteractor>()

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