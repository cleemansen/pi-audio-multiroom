package org.unividuell.pictl.server.usecase

class ChangeVolumeInteractor {

    interface DataSource {
        fun volumeStepUp(playerId: String)
        fun volumeStepDown(playerId: String)
    }

    fun volumeStepUp() {

    }
}