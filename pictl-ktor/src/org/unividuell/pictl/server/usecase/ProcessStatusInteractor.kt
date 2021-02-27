package org.unividuell.pictl.server.usecase

import org.kodein.di.DI
import org.kodein.di.instance

class ProcessStatusInteractor(di: DI) {

    private val repo: DataSource by di.instance()

    interface DataSource {

        data class ProcessInfo(
            val pid: Int?,
            val name: String
        )

        fun pid(processName: String): ProcessInfo

        fun serviceStatus(serviceName: String): String
    }

    enum class Service(val serviceName: String, val processName: String) {
        Pictl(serviceName = "pictl.service", processName = "java"),
        Squeezebox(serviceName = "logitechmediaserver.service", processName = "squeezeboxserve"),
        Squeezelite(serviceName = "squeezelite.service", processName = "squeezelite")
    }

    fun getProcessInfo(service: Service): DataSource.ProcessInfo {
        return repo.pid(processName = service.processName)
    }

    fun getServiceStatus(service: Service): String {
        return repo.serviceStatus(serviceName = service.serviceName)
    }

}