package org.unividuell.pictl.server.repository

import org.koin.core.component.KoinComponent
import org.unividuell.pictl.server.usecase.ChangeVolumeInteractor
import org.unividuell.pictl.server.usecase.TogglePlayPausePlayerInteractor

class SqueezeboxCometPlayerControlRepository :
    KoinComponent,
    SqueezeboxCometLongPollingRepository(),
    ChangeVolumeInteractor.DataSource,
    TogglePlayPausePlayerInteractor.DataSource {

    private val volumeStep = 4

    override fun volumeStepUp(playerId: String) {
        bayeuxClient.getChannel(SqueezeboxCometSubscriptionRepository.Channels.slimRequest)
            .publish(
                slimRequestData(
                    playerId = playerId,
                    command = listOf("mixer", "volume", "+$volumeStep")
                )
            )
    }

    override fun volumeStepDown(playerId: String) {
        bayeuxClient.getChannel(SqueezeboxCometSubscriptionRepository.Channels.slimRequest)
            .publish(
                slimRequestData(
                    playerId = playerId,
                    command = listOf("mixer", "volume", "-$volumeStep")
                )
            )
    }

    override fun volumeChange(playerId: String, desiredVolume: Int) {
        bayeuxClient.getChannel(SqueezeboxCometSubscriptionRepository.Channels.slimRequest)
            .publish(
                slimRequestData(
                    playerId = playerId,
                    command = listOf("mixer", "volume", "$desiredVolume")
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