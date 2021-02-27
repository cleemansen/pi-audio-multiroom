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

        fun processInfo(processName: String): List<ProcessInfo>
    }

    fun getProcessInfo(processName: String): List<DataSource.ProcessInfo> {
        return repo.processInfo(processName = processName)
    }

}