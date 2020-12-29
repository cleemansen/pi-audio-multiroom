package org.unividuell.pictl.server

import io.ktor.application.*
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
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.unividuell.pictl.server.controller.audioRoutes
import org.unividuell.pictl.server.controller.hardwareRoutes
import org.unividuell.pictl.server.controller.metricRoutes
import java.time.Duration

fun Application.audioModule(testing: Boolean = false) {

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

    routing {
        hardwareRoutes(di())
    }
}

fun Application.metricsModule(testing: Boolean = false) {

    val prometheusMeterRegistry: PrometheusMeterRegistry by di().instance()

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
        metricRoutes(di())
    }
}