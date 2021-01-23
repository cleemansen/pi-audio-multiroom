package org.unividuell.pictl.server.repository

import io.ktor.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.kodein.di.ktor.di
import org.unividuell.pictl.server.piCtl
import java.time.Duration
import kotlin.test.Test

class HardwareRepositoryTest {

    lateinit var sut: HardwareRepository

    @Test
    fun `it should respect the delay for shutdown`() {
        withTestApplication({
            piCtl(testing = true)
            (environment.config as MapApplicationConfig).apply {
                put("ktor.deployment.environment", "test")
                put("ktor.application.slimserver.host", "no://op")
            }
            sut = HardwareRepository(di = di())
        }) {
            runBlocking {
                sut.shutdownAsync(delay = Duration.ofSeconds(2.5.toLong())).await()
            }
        }
    }

}