package org.unividuell.pictl.server.repository

import io.ktor.application.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.isProd
import org.unividuell.pictl.server.usecase.ShutdownInteractor
import java.time.Duration
import kotlin.concurrent.timer

class HardwareRepository(di: DI) : ShutdownInteractor.DataSource {

    private val application: Application by di.instance()

    private val processIO: ProcessIO by di.instance()

    override fun shutdownAsync(delay: Duration): Deferred<Unit> {
        return GlobalScope.async { // launch a new coroutine in background and continue
            var remaining = delay
            val period = Duration.ofSeconds(1)
            timer(
                name = "shutdown",
                period = period.toMillis()
            ) {
                if (remaining.isZero || remaining.isNegative) {
                    application.log.info("shutdown now!")
                    this.cancel()
                } else {
                    remaining = remaining.minus(period)
                    application.log.info("shutdown in $remaining")
                }
            }
            delay(delay.toMillis())
            shutdownNow()
            application.log.info("shutdown scheduled...")
        }
    }

    private fun shutdownNow() {
        val processBuilder = ProcessBuilder()
        if (application.isProd) {
            processBuilder.command("sudo shutdown -P now 'shutdown requested by pictl'")
        } else {
            // shutdown -k simulates the command, good for testing?
            processBuilder.command("echo", "do nothing, not on PROD")

        }
        val process = processBuilder.start()
        processIO.inheritIO(src = process.inputStream, isError = false)
        processIO.inheritIO(src = process.errorStream, isError = true)
    }

}