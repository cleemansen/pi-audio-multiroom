package org.unividuell.pictl.server.usecase

import org.kodein.di.DI
import org.kodein.di.instance

class ServiceInteractor(di: DI) {

    private val repo: DataSource by di.instance()

    interface DataSource {

        data class ProcessInfo(
            val pid: Int?,
            val name: String
        )

        fun pid(processName: String): ProcessInfo

        fun serviceStatus(serviceName: String): String

        fun restartService(serviceNames: List<String>): Boolean
    }

    enum class Service(val serviceName: String, val processName: String) {
        Pictl(serviceName = "pictl.service", processName = "java"),
        Squeezebox(serviceName = "logitechmediaserver.service", processName = "squeezeboxserve"),
        Squeezelite(serviceName = "squeezelite.service", processName = "squeezelite");

        companion object {
            fun byServiceName(name: String?): Service? = values().find { it.serviceName == name }
        }
    }

    fun getProcessInfo(service: Service): DataSource.ProcessInfo {
        return repo.pid(processName = service.processName)
    }

    fun getServiceStatus(service: Service): String {
        return repo.serviceStatus(serviceName = service.serviceName)
    }

    fun restartService(services: List<Service>): Boolean {
        return repo.restartService(serviceNames = services.map { it.serviceName })
    }

}