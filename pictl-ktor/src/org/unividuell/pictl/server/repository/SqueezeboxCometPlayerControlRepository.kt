package org.unividuell.pictl.server.repository

import org.kodein.di.DI
import org.unividuell.pictl.server.usecase.ChangeVolumeInteractor
import org.unividuell.pictl.server.usecase.TogglePlayPausePlayerInteractor

class SqueezeboxCometPlayerControlRepository(di: DI) : SqueezeboxCometLongPollingRepository(di = di),
    ChangeVolumeInteractor.DataSource,
    TogglePlayPausePlayerInteractor.DataSource {

    private val volumeStep = 4

    override fun volumeStepUp(playerId: String) {
        bayeuxClient.getChannel(SqueezeboxCometSubscriptionRepository.Channels.slimRequest)
            .publish(
                slimRequestData(
                    playerId = playerId,
                    command = listOf("mixer volume +$volumeStep")
                )
            )
    }

    override fun volumeStepDown(playerId: String) {
        bayeuxClient.getChannel(SqueezeboxCometSubscriptionRepository.Channels.slimRequest)
            .publish(
                slimRequestData(
                    playerId = playerId,
                    command = listOf("mixer volume -$volumeStep")
                )
            )
    }

    override fun togglePlayPausePlayer(playerId: String) {
        bayeuxClient.getChannel(SqueezeboxCometSubscriptionRepository.Channels.slimRequest)
            .publish(
                slimRequestData(
                    playerId = playerId,
                    command = listOf("pause")
                )
            )
    }
}