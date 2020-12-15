package org.unividuell.pictl.server.repository

import org.unividuell.pictl.server.usecase.GetCurrentSongInteractor

class SlimboxRepository : GetCurrentSongInteractor.DataSource {

    override fun getCurrentSong(): GetCurrentSongInteractor.DataSource.CurrentSong? {
        return GetCurrentSongInteractor.DataSource.CurrentSong(title = "a DI test")
    }
}