package org.unividuell.pictl.server.usecase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TogglePlayPausePlayerInteractor : KoinComponent {

    private val requestPlayersUpdatesInteractor by inject<RequestPlayersUpdatesInteractor>()

    private val dataSource: DataSource by inject()

    interface DataSource {
        fun togglePlayPausePlayer(playerId: String)
    }

    fun toggle(playerId: String) {
        dataSource.togglePlayPausePlayer(playerId = playerId)
        // request instant update to get new player mode as soon as possible
        requestPlayersUpdatesInteractor.requestUpdate()
    }

}