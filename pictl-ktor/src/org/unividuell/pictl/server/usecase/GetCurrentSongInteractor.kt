package org.unividuell.pictl.server.usecase

import org.kodein.di.DI
import org.kodein.di.instance

class GetCurrentSongInteractor(di: DI) {

    private val repo: DataSource by di.instance()

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