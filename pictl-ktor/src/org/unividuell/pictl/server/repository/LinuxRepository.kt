package org.unividuell.pictl.server.repository

import io.ktor.application.*
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.usecase.ProcessStatusInteractor
import java.io.BufferedReader
import java.io.InputStreamReader

class LinuxRepository(di: DI) : ProcessStatusInteractor.DataSource {

    protected val application: Application by di.instance()

    override fun processInfo(processName: String): List<ProcessStatusInteractor.DataSource.ProcessInfo> {
        val pgrep = ProcessBuilder()
            .command("pgrep", processName)
            .start()
        val pids = mutableListOf<Int?>()
        BufferedReader(InputStreamReader(pgrep.inputStream)).useLines { lines ->
            lines.iterator().forEach { line ->
                try {
                    pids.add(line.toInt())
                } catch (noPid: NumberFormatException) {
                    application.log.debug("$line not parsable as PID")
                    pids.add(null)
                }
            }
        }
        pgrep.waitFor()
        if (pids.isEmpty()) {
            // process not found
            pids.add(null)
        }
        return pids.map {
            ProcessStatusInteractor.DataSource.ProcessInfo(
                pid = it,
                name = processName
            )
        }
    }
}