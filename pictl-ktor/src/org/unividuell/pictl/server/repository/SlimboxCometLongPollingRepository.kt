package org.unividuell.pictl.server.repository

import io.ktor.application.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.cometd.bayeux.Channel
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.client.BayeuxClient
import org.cometd.client.http.okhttp.OkHttpClientTransport
import org.cometd.client.transport.HttpClientTransport
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.network.cometd.CometOkHttpLogger
import org.unividuell.pictl.server.network.cometd.SqueezeboxCometConnectPatchInterceptor

class SlimboxCometLongPollingRepository(di: DI) {

    private val application: Application by di.instance()

    /** The maximum number of milliseconds to wait before considering a request to the LMS failed  */
    private val LONG_POLLING_TIMEOUT = 120000

    fun play() {
        val logging = HttpLoggingInterceptor(CometOkHttpLogger())
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val httpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(logging)
            .addInterceptor(SqueezeboxCometConnectPatchInterceptor())
            .build()

        val options = mutableMapOf<String, Any>()
        options[HttpClientTransport.MAX_NETWORK_DELAY_OPTION] = LONG_POLLING_TIMEOUT
        val httpTransport = OkHttpClientTransport(options, httpClient)

        val bayeuxClient = BayeuxClient("http://white.local:9000/cometd", httpTransport)

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
        application.log.info("establishing subscriptions..")

        bayeuxClient.getChannel(Channel.META_CONNECT).subscribe { channel, message ->
            application.log.info("received on ${channel.channelId}: $message [$channel]")
        }
        bayeuxClient.getChannel(Channel.META_SUBSCRIBE).subscribe { channel, message ->
            application.log.info("received on ${channel.channelId}: $message [$channel]")
        }
        bayeuxClient.getChannel(Channel.META_UNSUBSCRIBE).subscribe { channel, message ->
            application.log.info("received on ${channel.channelId}: $message [$channel]")
        }
        bayeuxClient.getChannel("/${bayeuxClient.id}/slim/playerstatus/myplayer").subscribe { channel, message ->
            application.log.info("received on ${channel.channelId}: $message [$channel]")
        }

//        val serverStatusReq = mapOf(
//            "request" to listOf(
//                "",
//                listOf(
//                    "serverstatus",
//                    "0",
//                    "255",
//                    "playerprefs:playtrackalbum,defeatDestructiveTouchToPlay",
//                    "prefs:mediadirs, defeatDestructiveTouchToPlay",
//                    "subscribe:30"
//                )
//            ),
//            "response" to "/${bayeuxClient.id}/slim/serverstatus"
//        )

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
    }


}