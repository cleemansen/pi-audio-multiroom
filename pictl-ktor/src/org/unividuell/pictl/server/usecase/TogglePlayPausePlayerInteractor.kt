package org.unividuell.pictl.server.usecase

import org.kodein.di.DI
import org.kodein.di.instance

class TogglePlayPausePlayerInteractor(
    di: DI
) {

    interface DataSource {
        fun togglePlayPausePlayer(playerId: String)
    }

    private val dataSource: DataSource by di.instance()

    fun toggle(playerId: String) {
        dataSource.togglePlayPausePlayer(playerId = playerId)
    }

}