package org.unividuell.pictl.server.repository

import io.ktor.application.*
import okhttp3.internal.toImmutableList
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.usecase.ProcessStatusInteractor
import java.io.BufferedReader
import java.io.InputStreamReader

class LinuxRepository(di: DI) : ProcessStatusInteractor.DataSource {

    protected val application: Application by di.instance()

    override fun pid(processName: String): ProcessStatusInteractor.DataSource.ProcessInfo {
        return pidBySystemd(processName).let {
            ProcessStatusInteractor.DataSource.ProcessInfo(
                pid = it,
                name = processName
            )
        }
    }

    override fun serviceStatus(serviceName: String): String {
        return statusBySystemd(serviceName = serviceName)
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