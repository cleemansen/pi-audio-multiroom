package org.unividuell.pictl.server

import io.ktor.application.*

val Application.envKind get() = environment.config.property("ktor.deployment.environment").getString()
val Application.isDev get() = envKind == "dev"
val Application.isTest get() = envKind == "test"
val Application.isProd get() = !isDev && !isTest