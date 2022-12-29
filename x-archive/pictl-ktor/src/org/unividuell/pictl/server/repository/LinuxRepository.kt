package org.unividuell.pictl.server.repository

import io.ktor.application.*
import okhttp3.internal.toImmutableList
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.unividuell.pictl.server.usecase.ServiceInteractor
import java.io.BufferedReader
import java.io.InputStreamReader

class LinuxRepository : KoinComponent, ServiceInteractor.DataSource {

    private val application: Application by inject()

    private val processIO: ProcessIO by inject()

    override fun pid(processName: String): ServiceInteractor.DataSource.ProcessInfo {
        return pidBySystemd(processName).let {
            ServiceInteractor.DataSource.ProcessInfo(
                pid = it,
                name = processName
            )
        }
    }

    override fun serviceStatus(serviceName: String): String {
        return statusBySystemd(serviceName = serviceName)
    }

    override fun restartService(serviceNames: List<String>): Boolean {
        val cmd = listOf("sudo", "systemctl", "restart").plus(serviceNames)
        application.log.info("executing `$cmd`")
        val process = ProcessBuilder()
            .command(cmd)
            .start()
        processIO.inheritIO(src = process.inputStream, isError = false)
        processIO.inheritIO(src = process.errorStream, isError = true)
        val result = process.waitFor()
        if (result != 0) {
            application.log.warn("restart was not successful - exit code $result")
        }
        return result == 0
    }

    private fun statusBySystemd(serviceName: String): String {
        val process = ProcessBuilder()
            .command("systemctl", "status", serviceName)
            .start()
        val result = mutableListOf<String>()
        BufferedReader(InputStreamReader(process.inputStream)).useLines {
            it.iterator().forEach { line -> result.add(line) }
        }
        process.waitFor()
        return result.joinToString(separator = "\n")
    }

    private fun showBySystemd(vararg serviceNames: String) {
        val cmd = listOf(
            "systemctl", "show",
            "-p", "Id",
            "-p", "Description",
            "-p", "MainPID",
            "-p", "ActiveState",
            "-p", "StateChangeTimestamp"
        ).plus(serviceNames)
    }

    private fun isActiveBySystemd(vararg serviceNames: String): List<String> {
        val cmd = listOf("systemctl", "is-active").plus(serviceNames)
        val process = ProcessBuilder()
            .command(cmd)
            .start()
        val states = mutableListOf<String>()
        BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
            lines.iterator().forEach { line ->
                states.add(line)
            }
        }
        process.waitFor()
        return states.toImmutableList()
    }

    private fun pidBySystemd(serviceName: String): Int? {
        val process = ProcessBuilder()
            .command("systemctl", "show", "--property", "MainPID", "--value", serviceName)
            .start()
        var pid: Int? = null
        BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
            lines.iterator().forEach { line ->
                try {
                    pid = line.toInt()
                } catch (noPid: NumberFormatException) {
                    application.log.debug("$line not parsable as PID")
                }
            }
        }
        process.waitFor()
        return pid
    }

    private fun pidByPgrep(processName: String): List<Int?> {
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
        return pids.toImmutableList()
    }
}