package org.unividuell.pictl.server.usecase

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.koin.dsl.module

internal class ChangeVolumeInteractorTest : InteractorTestBase() {

    val mockDataSource = mockk<ChangeVolumeInteractor.DataSource>(relaxed = true)
    val mockRequestPlayersUpdatesInteractor = mockk<RequestPlayersUpdatesInteractor>(relaxed = true)

    val sut: ChangeVolumeInteractor by lazy { ChangeVolumeInteractor() }

    init {
        startInjection(
            module {
                single { mockDataSource }
                single { mockRequestPlayersUpdatesInteractor }
            }
        )
    }

    @Test
    internal fun `volume step up should call the data-source and request a player-update`() {
        // execute
        sut.volumeStepUp(playerId = "p-id")
        // verify
        verify {
            mockDataSource.volumeStepUp(playerId = "p-id")
            mockRequestPlayersUpdatesInteractor.requestUpdate()
        }
    }

    @Test
    internal fun `volume step down should call the data-source and request a player-update`() {
        // execute
        sut.volumeStepDown(playerId = "p-id")
        // verify
        verify {
            mockDataSource.volumeStepDown(playerId = "p-id")
            mockRequestPlayersUpdatesInteractor.requestUpdate()
        }
    }

    @Test
    internal fun `volume change should call the data-source and request a player-update`() {
        // execute
        sut.volumeChange(playerId = "p-id", desiredVolume = 58)
        // verify
        verify {
            mockDataSource.volumeChange(playerId = "p-id", desiredVolume = 58)
            mockRequestPlayersUpdatesInteractor.requestUpdate()
        }
    }
}