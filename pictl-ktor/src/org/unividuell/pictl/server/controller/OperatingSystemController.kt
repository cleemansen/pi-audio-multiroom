package org.unividuell.pictl.server.controller

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.isProd
import org.unividuell.pictl.server.usecase.ServiceInteractor

fun Routing.operatingSystemRoutes(di: DI) {

    val serviceInteractor: ServiceInteractor by di.instance()

    val services = listOf(
        ServiceInteractor.Service.Squeezelite,
        ServiceInteractor.Service.Squeezebox,
        ServiceInteractor.Service.Pictl
    )

    route("/ctl-os") {
        route("/services") {
            get("/") {
                val serviceStatus = services.map {
                    it to if (application.isProd) {
                        serviceInteractor.getServiceStatus(service = it)
                    } else {
                        "not available on build machine.."
                    }
                }
                call.respondHtml {
                    head {
                        title("Services")
                    }
                    body {
                        h1 { +"Service Overview" }
                        form(action = "/ctl-os/services/restart", method = FormMethod.post) {
                            submitInput { value = "RESTART" }
                            serviceStatus.forEach {
                                h2 { +it.first.serviceName }
                                checkBoxInput(name = "service") {
                                    id = it.first.serviceName
                                    value = it.first.serviceName
                                    label {
                                        htmlFor = it.first.serviceName
                                        +"restart"
                                    }
                                }
                                pre { +it.second }
                            }
                        }
                    }
                }
            }
            post("/restart") {
                val services = call.receiveParameters()
                    .getAll("service")
                    ?.mapNotNull { ServiceInteractor.Service.byServiceName(name = it) }
                if (services == null) {
                    call.respond(status = HttpStatusCode.BadRequest, message = "no / invalid service(s) provided!")
                } else {
                    val result = serviceInteractor.restartService(services = services)
                    if (result) {
                        call.respondRedirect(url = "/ctl-os/services")
                    } else {
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = "restart failed!"
                        )
                    }
                }
            }
        }
    }
}