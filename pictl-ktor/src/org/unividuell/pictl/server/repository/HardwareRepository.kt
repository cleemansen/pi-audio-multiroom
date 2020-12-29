package org.unividuell.pictl.server.repository

import io.ktor.application.*
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.isProd
import org.unividuell.pictl.server.usecase.ShutdownInteractor
import java.time.Duration

class HardwareRepository(di: DI) : ShutdownInteractor.DataSource {

    private val application: Application by di.instance()

    override fun shutdown(delay: Duration) {
        val process = ProcessBuilder()
            .inheritIO()

        if (application.isProd) {
            // the shutdown only accepts minutes/hours :/
            process.command("sleep ${delay.seconds};sudo shutdown now")
        } else {
            process.command("echo", delay.toString())

        }

        process.start()
    }

}