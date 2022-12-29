package org.unividuell.pictl.server.usecase

import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.koin.dsl.module

internal class GetCurrentSongInteractorTest : InteractorTestBase() {

    @RelaxedMockK
    lateinit var mockDataSource: GetCurrentSongInteractor.DataSource

    val sut: GetCurrentSongInteractor by lazy { GetCurrentSongInteractor() }

    init {
        startInjection(
            module {
                single { mockDataSource }
            }
        )
    }

    @Test
    internal fun `it should return the current song`() {
        // prepare
        val currentSong = mockk<GetCurrentSongInteractor.DataSource.CurrentSong>()
        every { mockDataSource.getCurrentSong() } returns currentSong

        // execute
        val actual = sut.getCurrentSong()

        // verify
        assertThat(actual).isEqualTo(currentSong)
    }

    @Test
    internal fun `it should return null`() {
        // prepare
        every { mockDataSource.getCurrentSong() } returns null

        // execute
        val actual = sut.getCurrentSong()

        // verify
        assertThat(actual).isNull()
    }
}