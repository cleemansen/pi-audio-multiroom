package org.unividuell.pictl.server

import io.ktor.application.*
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
import org.unividuell.pictl.server.controller.metricRoutes

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