package org.unividuell.pictl.server.controller

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import org.kodein.di.DI
import java.net.InetAddress

fun Routing.hardwareRoutes(
    di: DI
) {

    route("/hardware") {
        get("/shutdown") {
            // backup
            call.respondHtml {
                head {
                    title("shutdown backup")
                }
                body {
                    h1 { +"Shutdown" }
                    form(action = "/hardware/shutdown/me", method = FormMethod.post) {
                        submitInput { value = "SHUTDOWN" }
                    }
                }
            }
        }
        post("/shutdown/me") {
            application.log.info("shutting down myself now")
            call.respondText { "ok" }
        }
        post("/shutdown") {
            call.request.queryParameters["delay"]
            call.respondText {
                "ok"
            }
        }
    }
}

data class ShutdownViewModel(
    val ips: List<InetAddress>,
    val delay: Long
)