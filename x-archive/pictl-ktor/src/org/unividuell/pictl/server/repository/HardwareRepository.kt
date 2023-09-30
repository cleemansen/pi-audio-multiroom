package org.unividuell.pictl.server.repository

import io.ktor.application.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.unividuell.pictl.server.isProd
import org.unividuell.pictl.server.usecase.ShutdownInteractor
import java.time.Duration
import java.util.*
import kotlin.concurrent.timer

class HardwareRepository : KoinComponent, ShutdownInteractor.DataSource {

    private val logger = KotlinLogging.logger { }

    private val application: Application by inject()

    private val processIO: ProcessIO by inject()

    override fun shutdown(delay: Duration): Timer {
        var remaining = delay
        val period = Duration.ofSeconds(1)
        return timer(
            name = "shutdown",
            period = period.toMillis(),
        ) {
            if (remaining.isZero || remaining.isNegative) {
                logger.info("shutdown now!")
                shutdownNow()
                logger.info("shutdown scheduled...")
                this.cancel()
            } else {
                remaining = remaining.minus(period)
                logger.info("shutdown in $remaining")
            }
        }
    }

    private fun shutdownNow() {
        val processBuilder = ProcessBuilder()
        if (application.isProd) {
            processBuilder.command("sudo", "shutdown", "-P", "now")
        } else {
            // shutdown -k simulates the command, good for testing?
            processBuilder.command("echo", "do nothing, we are not on PROD")

        }
        val process = processBuilder.start()
        processIO.inheritIO(src = process.inputStream, isError = false)
        processIO.inheritIO(src = process.errorStream, isError = true)
    }

}