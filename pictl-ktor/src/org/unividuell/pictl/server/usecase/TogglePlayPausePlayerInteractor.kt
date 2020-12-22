package org.unividuell.pictl.server.usecase

import org.kodein.di.DI
import org.kodein.di.instance

class TogglePlayPausePlayerInteractor(
    di: DI
) {

    private val subscribeForPlayersUpdatesInteractor: SubscribeForPlayersUpdatesInteractor by di.instance()

    private val dataSource: DataSource by di.instance()

    interface DataSource {
        fun togglePlayPausePlayer(playerId: String)
    }

    fun toggle(playerId: String) {
        dataSource.togglePlayPausePlayer(playerId = playerId)
        // request instant update to get new player mode as soon as possible
        subscribeForPlayersUpdatesInteractor.requestUpdate()
    }

}