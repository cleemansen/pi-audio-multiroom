package org.unividuell.pictl.server.controller

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.routing.*
import kotlinx.html.*
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.usecase.ProcessStatusInteractor

fun Routing.operatingSystemRoutes(di: DI) {

    val processStatusInteractor: ProcessStatusInteractor by di.instance()

    val squeezeliteProcessName = "squeezelite"
    val squeezeboxProcessName = "squeezeboxserve"
    val javaProcessName = "java"

    val processNamesForStatus = listOf(squeezeboxProcessName, squeezeliteProcessName, javaProcessName)

    route("/ctl-os") {
        get("/status") {
            val processes = processNamesForStatus.map {
                processStatusInteractor.getProcessInfo(processName = it)
            }
            call.respondHtml {
                head {
                    title("Processes")
                }
                body {
                    h1 { +"Process Overview" }
                    table {
                        thead {
                            tr { listOf(th { +"process" }, th { +"PID" }) }
                        }
                        tbody {
                            listOf(
                                processNamesForStatus.forEach { processName ->
                                    processStatusInteractor.getProcessInfo(processName = processName).map { process ->
                                        tr { listOf(td { +process.name }, td { +process.pid.toString() }) }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}