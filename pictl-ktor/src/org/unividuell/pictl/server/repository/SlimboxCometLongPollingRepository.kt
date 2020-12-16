package org.unividuell.pictl.server.repository

import io.ktor.application.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.cometd.bayeux.Channel
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.client.BayeuxClient
import org.cometd.client.http.okhttp.OkHttpClientTransport
import org.cometd.client.transport.HttpClientTransport
import org.cometd.client.websocket.okhttp.OkHttpWebSocketTransport
import org.kodein.di.DI
import org.kodein.di.instance
import org.slf4j.LoggerFactory
import kotlin.concurrent.fixedRateTimer

class SlimboxCometLongPollingRepository(di: DI) {

    private val application: Application by di.instance()

    /** The maximum number of milliseconds to wait before considering a request to the LMS failed  */
    private val LONG_POLLING_TIMEOUT = 120000

    fun play() {
        val logging = HttpLoggingInterceptor(MyOkHttpLogger())
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val httpClient = OkHttpClient.Builder().addNetworkInterceptor(logging).build()
        val wsTransport = OkHttpWebSocketTransport(null, httpClient)

        val options = mutableMapOf<String, Any>()
        options[HttpClientTransport.MAX_NETWORK_DELAY_OPTION] = LONG_POLLING_TIMEOUT
        options[HttpClientTransport.MAX_NETWORK_DELAY_OPTION] = LONG_POLLING_TIMEOUT
        val httpTransport = OkHttpClientTransport(options, httpClient)

        val bayeuxClient = BayeuxClient("http://localhost:9000/cometd", httpTransport)

        bayeuxClient.getChannel("/meta/connect").subscribe { channel, message ->
            application.log.info("Something happen on the META_CONNECT channel: $message")
        }
        bayeuxClient.getChannel(Channel.META_HANDSHAKE)
            .addListener(ClientSessionChannel.MessageListener { channel, message ->
                if (message.isSuccessful) {
                    establishSubscriptions(bayeuxClient)
                } else {
                    application.log.error("Could not establish HANDSHAKE: $message")
                }
            })

        bayeuxClient.handshake()
    }

    private fun establishSubscriptions(bayeuxClient: BayeuxClient) {
        application.log.info("establish subscriptions..")

        bayeuxClient.getChannel("/meta/connect").subscribe { channel, message ->
            application.log.info("received on ${channel.channelId}: $message [$channel]")
        }
        bayeuxClient.getChannel("/${bayeuxClient.id}/slim/serverstatus").subscribe { channel, message ->
            application.log.info("received on ${channel.channelId}: $message [$channel]")
        }
        bayeuxClient.getChannel("/${bayeuxClient.id}/slim/playerstatus/myplayer").subscribe { channel, message ->
            application.log.info("received on ${channel.channelId}: $message [$channel]")
        }

        val serverStatusReq = mapOf(
            "request" to listOf(
                "",
                listOf(
                    "serverstatus",
                    "0",
                    "255",
                    "playerprefs:playtrackalbum,defeatDestructiveTouchToPlay",
                    "prefs:mediadirs, defeatDestructiveTouchToPlay",
                    "subscribe:30"
                )
            ),
            "response" to "/${bayeuxClient.id}/slim/serverstatus"
        )
        bayeuxClient
            .getChannel("/slim/subscribe")
            .publish(serverStatusReq) { application.log.info("I REQUESTED the serverstatus: $it") }


        val playerStatusReq = mapOf(
            "request" to listOf(
                "b8:27:eb:44:2f:38",
                listOf("status", "-", "1", "useContextMenu:1", "subscribe:600", "menu:menu", "subscribe:10")
            ),
            "response" to "/${bayeuxClient.id}/slim/playerstatus/myplayer"
        )
        bayeuxClient
            .getChannel("/slim/subscribe")
            .publish(playerStatusReq) { application.log.info("I REQUESTED the playerstatus: $it") }


        fixedRateTimer(
            name = "heartbeat",
            initialDelay = 0,
            period = 60_000
        ) {
            bayeuxClient
                .getChannel("/meta/connect")
                .publish(
                    mapOf(
                        "request" to emptyList<String>()
                    )
                ) { application.log.info("HEARTBEAT response: $it") }
        }
        // END
    }

    class MyOkHttpLogger : HttpLoggingInterceptor.Logger {
        private val logger = LoggerFactory.getLogger(MyOkHttpLogger::class.java)
        override fun log(message: String) {
            logger.info(message)
        }

    }


}