package org.unividuell.pictl.server.controller

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.html.*
import org.koin.ktor.ext.inject
import org.unividuell.pictl.server.usecase.ShutdownInteractor
import java.net.InetSocketAddress
import java.time.Duration
import java.time.format.DateTimeParseException

fun Routing.hardwareRoutes() {

    val shutdownInteractor: ShutdownInteractor by inject()

    route("/ctl-hardware") {
        get("/shutdown/me") {
            // backup
            call.respondHtml {
                head {
                    title("shutdown backup")
                }
                body {
                    h1 { +"Shutdown" }
                    form(action = "/ctl-hardware/shutdown/me", method = FormMethod.post) {
                        submitInput { value = "SHUTDOWN" }
                    }
                }
            }
        }
        post("/shutdown/me") {
            application.log.info("shutting down myself now")
            val delayParam = call.request.queryParameters["delay"]
            val delay = try {
                if (delayParam?.isNotEmpty() == true) {
                    Duration.parse(delayParam)
                } else {
                    null
                }
            } catch (okay: DateTimeParseException) {
                application.log.warn("ignore not parsable duration for delay: $delayParam")
                null
            }
            CoroutineScope(Dispatchers.IO).launch {
                shutdownInteractor.shutdownMe(delay = delay)
            }
            call.respondText {
                "ok"
            }

        }
        post("/shutdown") {
            val body = withContext(context = Dispatchers.IO) {
                call.receive<ShutdownViewModel>()
            }
            val delay = if (body.delayMillis != null) {
                Duration.ofMillis(body.delayMillis)
            } else {
                null
            }
            shutdownInteractor.shutdownNodes(ips = body.ips, delay = delay)
            call.respondText { "ok" }
        }
    }
}

data class ShutdownViewModel(
    val ips: List<InetSocketAddress>,
    val delayMillis: Long?
)