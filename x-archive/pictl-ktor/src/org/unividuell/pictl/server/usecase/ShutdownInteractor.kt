package org.unividuell.pictl.server.usecase

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.InetSocketAddress
import java.time.Duration
import java.util.*

class ShutdownInteractor : KoinComponent {

    interface DataSource {
        fun shutdown(delay: Duration): Timer
        fun beep(frequency: Int, durationSeconds: Float = 0.2f)
    }

    private val logger = KotlinLogging.logger { }

    private val dataSource: DataSource by inject()

    private val httpClient: HttpClient by inject()

    private val application: Application by inject()

    private val shutdownDelayFallback = Duration.ofSeconds(10)

    private var shutdownTimer: Timer? = null

    // we assume that ALL pi-ctl nodes runs on the same port!
    private val piCtlPort: Int = application.environment.config.property("ktor.deployment.port").getString().toInt()

    suspend fun shutdownNodes(ips: List<InetSocketAddress>, delay: Duration? = null) {
        ips.forEach { node ->
            val response = httpClient.request<String> {
                url {
                    method = HttpMethod.Post
                    protocol = URLProtocol.HTTP
                    host = node.hostString
                    port = piCtlPort
                    encodedPath = "ctl-hardware/shutdown/me"
                    parameters.append("delay", delay.toString())
                }
            }
            logger.info("Shutdown response from $node: $response [request gone to ${node.hostString}]")
        }
    }

    fun shutdownMe(delay: Duration? = null) {
        shutdownTimer?.run {
            this.cancel()
            shutdownTimer = null
            logger.info("shutdown aborted!")
            dataSource.beep(frequency = 200, durationSeconds = 2F)
            return
        }
        shutdownTimer = dataSource.shutdown(delay = delay ?: shutdownDelayFallback)
    }
}