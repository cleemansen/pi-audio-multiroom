package org.unividuell.pictl.server.repository

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.*

class ProcessIO {

    private val appLogger: Logger = LoggerFactory.getLogger(this::class.java)

    fun inheritIO(src: InputStream, isError: Boolean) {
        Thread {
            val sc = Scanner(src)
            while (sc.hasNextLine()) {
                val line = sc.nextLine()
                if (isError) {
                    appLogger.error("(OS) $line")
                } else {
                    // parse $line and use correct loglevel method
                    val result = Regex("^\\[(TRACE|DEBUG|INFO|ERROR|WARN|FATAL)\\](.*)").find(line)
                    var logLine = "(OS) $line"
                    if (result == null || result.groupValues.size <= 1) {
                        appLogger.info(logLine)
                    } else {
                        logLine = "(OS) ${result.groupValues[2].trim()}"
                        when (result.groupValues[1]) {
                            "TRACE" -> appLogger.trace(logLine)
                            "INFO" -> appLogger.info(logLine)
                            "DEBUG" -> appLogger.debug(logLine)
                            "WARN" -> appLogger.warn(logLine)
                            "ERROR" -> appLogger.error(logLine)
                            "FATAL" -> appLogger.error(logLine)
                            else -> appLogger.info(logLine)
                        }
                    }
                }
            }
        }.start()
    }
}
