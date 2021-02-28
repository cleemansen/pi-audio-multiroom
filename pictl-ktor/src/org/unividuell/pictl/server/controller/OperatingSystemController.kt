package org.unividuell.pictl.server.controller

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.routing.*
import kotlinx.html.*
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.usecase.ServiceInteractor

fun Routing.operatingSystemRoutes(di: DI) {

    val serviceInteractor: ServiceInteractor by di.instance()

    val services = listOf(
        ServiceInteractor.Service.Squeezelite,
        ServiceInteractor.Service.Squeezebox,
        ServiceInteractor.Service.Pictl
    )

    route("/ctl-os") {
        get("/status") {
            val serviceStatus = services.map {
                it to serviceInteractor.getServiceStatus(service = it)
            }
            call.respondHtml {
                head {
                    title("Services")
                }
                body {
                    h1 { +"Service Overview" }
                    serviceStatus.forEach {
                        h2 { +it.first.serviceName }
                        pre { +it.second }
                    }
                }
            }
        }
    }
}