package org.unividuell.pictl.server

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.http.cio.websocket.*
import io.ktor.metrics.micrometer.*
import io.ktor.routing.*
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.cometd.client.BayeuxClient
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.unividuell.pictl.server.controller.audioRoutes
import org.unividuell.pictl.server.controller.hardwareRoutes
import org.unividuell.pictl.server.controller.metricRoutes
import org.unividuell.pictl.server.controller.operatingSystemRoutes
import org.unividuell.pictl.server.network.cometd.SqueezeboxBayeuxDefaultClient
import org.unividuell.pictl.server.repository.*
import org.unividuell.pictl.server.usecase.*
import java.time.Duration

fun Application.audioModule(testing: Boolean = false) {
    log.info("starting module `audio`")
    install(io.ktor.websocket.WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        audioRoutes()
    }
}

fun Application.hardwareModule(testing: Boolean = false) {
    log.info("starting module `hardware`")
    routing {
        hardwareRoutes()
    }
}

fun Application.osModule(testing: Boolean = false) {
    log.info("starting module `OS`")
    routing {
        operatingSystemRoutes()
    }
}

fun Application.metricsModule(testing: Boolean = false) {
    log.info("starting module `metrics`")
    val prometheusMeterRegistry: PrometheusMeterRegistry by inject()

    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
        meterBinders = listOf(
            ClassLoaderMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            JvmThreadMetrics(),
            FileDescriptorMetrics(),
            UptimeMetrics()
        )
    }

    routing {
        metricRoutes()
    }
}

val applicationModule = module {
    // frameworks
    val client = HttpClient(CIO) {
        install(HttpTimeout) {
        }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
        install(Logging) {
            level = LogLevel.ALL
        }
    }
    single<HttpClient> { client }
    single<BayeuxClient> { SqueezeboxBayeuxDefaultClient().buildBayeuxClient() }
    single<PrometheusMeterRegistry> { PrometheusMeterRegistry(PrometheusConfig.DEFAULT) }
}
val useCaseModule = module {
    // interactors
    single<GetCurrentSongInteractor.DataSource> { SqueezeboxJsonRpcRepository() }
    single<GetCurrentSongInteractor> { GetCurrentSongInteractor() }
    single<SubscribeForPlayersUpdatesInteractor.DataSource> { SqueezeboxCometSubscriptionRepository() }
    single<SubscribeForPlayersUpdatesInteractor> { SubscribeForPlayersUpdatesInteractor() }
    single<RequestPlayersUpdatesInteractor.DataSource> { SqueezeboxCometSubscriptionRepository() }
    single<RequestPlayersUpdatesInteractor> { RequestPlayersUpdatesInteractor() }
    single<TogglePlayPausePlayerInteractor.DataSource> { SqueezeboxCometPlayerControlRepository() }
    single<TogglePlayPausePlayerInteractor> { TogglePlayPausePlayerInteractor() }
    single<ChangeVolumeInteractor.DataSource> { SqueezeboxCometPlayerControlRepository() }
    single<ChangeVolumeInteractor> { ChangeVolumeInteractor() }
    single<ShutdownInteractor.DataSource> { HardwareRepository() }
    single<ProcessIO> { ProcessIO() }
    single<ShutdownInteractor> { ShutdownInteractor() }
    single<ServiceInteractor.DataSource> { LinuxRepository() }
    single<ServiceInteractor> { ServiceInteractor() }
}