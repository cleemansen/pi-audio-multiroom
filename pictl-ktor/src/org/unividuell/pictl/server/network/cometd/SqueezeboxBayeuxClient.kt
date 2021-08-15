package org.unividuell.pictl.server.network.cometd

import io.ktor.application.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.cometd.client.BayeuxClient
import org.cometd.client.http.okhttp.OkHttpClientTransport
import org.cometd.client.transport.HttpClientTransport
import org.cometd.common.JacksonJSONContextClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SqueezeboxBayeuxClient : KoinComponent {

    private val application: Application by inject()

    private val slimserverHost = application.environment.config.property("ktor.application.slimserver.host").getString()

    fun buildBayeuxClient(): BayeuxClient {
        val logging = HttpLoggingInterceptor(CometOkHttpLogger())
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(SqueezeboxCometConnectPatchInterceptor())
//            .addNetworkInterceptor(SqueezeboxCometGzipPatchInterceptor())
            .addNetworkInterceptor(logging)
            .build()

        // The maximum number of milliseconds to wait before considering a request to the LMS failed
        val longPollingTimeout = 30_123  // an odd number to uniquely identify timeouts by this option
        val options = mutableMapOf<String, Any>()
        options[HttpClientTransport.MAX_NETWORK_DELAY_OPTION] = longPollingTimeout
        val jsonContext = JacksonJSONContextClient()
        options[HttpClientTransport.JSON_CONTEXT_OPTION] = jsonContext
        val httpTransport = OkHttpClientTransport(options, httpClient)

        return BayeuxClient("$slimserverHost/cometd", httpTransport)
    }
}