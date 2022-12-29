package org.unividuell.pictl.server.network.cometd

import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory

class CometOkHttpLogger : HttpLoggingInterceptor.Logger {
    private val logger = LoggerFactory.getLogger(CometOkHttpLogger::class.java)
    override fun log(message: String) {
        logger.info(message)
    }
}