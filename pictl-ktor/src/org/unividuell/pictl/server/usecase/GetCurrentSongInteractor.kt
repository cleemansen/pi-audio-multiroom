package org.unividuell.pictl.server.usecase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GetCurrentSongInteractor : KoinComponent {

    private val repo: DataSource by inject()

    interface DataSource {
        data class CurrentSong(
            val artist: String?,
            val title: String?,
            val album: String?
        )

        fun getCurrentSong(): CurrentSong?
    }

    fun getCurrentSong(): DataSource.CurrentSong? {
        return repo.getCurrentSong()
    }

}