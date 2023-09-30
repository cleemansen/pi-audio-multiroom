package org.unividuell.pictl.server.usecase

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import java.net.InetSocketAddress
import java.time.Duration

internal class ShutdownInteractorTest : InteractorTestBase() {

    @RelaxedMockK
    lateinit var mockDataSource: ShutdownInteractor.DataSource

    @RelaxedMockK
    lateinit var mockApplication: Application

    private val hitNodes = mutableListOf<Url>()
    private val mockClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
//                when (request.url.fullPath) {
//                    "/ctl-hardware/shutdown/me?delay=null" -> {
                hitNodes.add(request.url)
                respond("okay")
//                    }
//                    else -> error("Unhandled ${request}")
//                }
            }
        }
    }

    private val sut: ShutdownInteractor by lazy { ShutdownInteractor() }

    init {
        startInjection(
            module {
                single { mockDataSource }
                single { mockApplication }
                single { mockClient }
            }
        )
    }

    @BeforeEach
    internal fun setUp() {
        every { mockApplication.environment } returns mockk {
            every { config } returns mockk(relaxed = true) {
                every { property("ktor.deployment.port") } returns mockk {
                    every { getString() } returns "58"
                }
            }
        }
        hitNodes.clear()
    }

    @Test
    internal fun `it should shutdown all nodes`() = runBlockingTest {
        // execute
        sut.shutdownNodes(
            ips = listOf(
                InetSocketAddress("example.com", 5858),
                InetSocketAddress("example.com", 9999),
            ),
            delay = Duration.ofMinutes(2)
        )
        // verify
        assertThat(hitNodes.map { it.toString() })
            .containsExactly(
                "http://example.com:58/ctl-hardware/shutdown/me?delay=PT2M",
                "http://example.com:58/ctl-hardware/shutdown/me?delay=PT2M"
            )
    }

    @Test
    internal fun `it should shutdown itself with default delay`() {
        // execute
        sut.shutdownMe()
        // verify
        verify { mockDataSource.shutdown(delay = Duration.ofSeconds(10)) }
    }

    @Test
    internal fun `it should shutdown itself with given delay`() {
        // execute
        sut.shutdownMe(delay = Duration.ofMinutes(5))
        // verify
        verify { mockDataSource.shutdown(delay = Duration.ofMinutes(5)) }
    }

    @Test
    internal fun `it should abort the shutdown`() {
        sut.shutdownMe(delay = Duration.ofMinutes(0))
        // execute
        sut.shutdownMe(delay = Duration.ofSeconds(15))
        // verify
        verify(exactly = 1) { mockDataSource.shutdown(delay = Duration.ofMinutes(0)) }
    }
}