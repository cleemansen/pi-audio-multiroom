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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

interface SqueezeboxBayeuxClient {
    fun buildBayeuxClient(): BayeuxClient
}

abstract class SqueezeboxBayeuxClientBase : SqueezeboxBayeuxClient, KoinComponent {

    private val application: Application by inject()

    protected val slimserverHost =
        application.environment.config.property("ktor.application.slimserver.host").getString()

    protected fun transportOptions(): MutableMap<String, Any> {
        // The maximum number of milliseconds to wait before considering a request to the LMS failed
        // The LMS prints this: `Slim::Web::Cometd::handler (305) Waiting 60 seconds on long-poll connection`
        // so perhaps timout > 60s is a good choice?!
        val longPollingTimeout = 60_123  // an odd number to uniquely identify timeouts by this option
        val options = mutableMapOf<String, Any>()
        options[HttpClientTransport.MAX_NETWORK_DELAY_OPTION] = longPollingTimeout
        val jsonContext = JacksonJSONContextClient()
        options[HttpClientTransport.JSON_CONTEXT_OPTION] = jsonContext

        return options
    }

}

class SqueezeboxBayeuxDefaultClient : SqueezeboxBayeuxClientBase() {

    override fun buildBayeuxClient(): BayeuxClient {
        val logging = HttpLoggingInterceptor(CometOkHttpLogger())
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(SqueezeboxCometConnectPatchInterceptor())
//            .addNetworkInterceptor(SqueezeboxCometGzipPatchInterceptor())
            .addNetworkInterceptor(logging)
            .build()

        val httpTransport = OkHttpClientTransport(transportOptions(), httpClient)

        return BayeuxClient("$slimserverHost/cometd", httpTransport)
    }

}

class SqueezeboxBayeuxTrustEveryTlsCertClient : SqueezeboxBayeuxClientBase() {

    override fun buildBayeuxClient(): BayeuxClient {
        val logging = HttpLoggingInterceptor(CometOkHttpLogger())
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(trustEveryCerts), SecureRandom())

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(SqueezeboxCometConnectPatchInterceptor())
//            .addNetworkInterceptor(SqueezeboxCometGzipPatchInterceptor())
            .addNetworkInterceptor(logging)
            // trust all SSL/TLS certificates. this is normally not needed as we use real and valid LE certificates
            .sslSocketFactory(sslContext.socketFactory, trustEveryCerts)
            .hostnameVerifier { _, _ -> true }
            // END trust all SSL/TLS certificates.
            .build()

        val httpTransport = OkHttpClientTransport(transportOptions(), httpClient)

        return BayeuxClient("$slimserverHost/cometd", httpTransport)
    }

    private val trustEveryCerts = object : X509TrustManager {
        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
        override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
    }
}