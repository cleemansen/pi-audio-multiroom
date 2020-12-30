package org.unividuell.pictl.server.usecase

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.kodein.di.DI
import org.kodein.di.instance
import java.net.InetSocketAddress
import java.time.Duration

class ShutdownInteractor(di: DI) {

    interface DataSource {
        fun shutdown(delay: Duration)
    }

    private val dataSource: DataSource by di.instance()

    private val httpClient: HttpClient by di.instance()

    private val application: Application by di.instance()

    private val shutdownDelayFallback = Duration.ofSeconds(10)

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
            application.log.info("Shutdown response from $node: $response [request gone to ${node.hostString}]")
        }
    }

    fun shutdownMe(delay: Duration? = null) {
        dataSource.shutdown(delay = delay ?: shutdownDelayFallback)
    }
}