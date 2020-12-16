package org.unividuell.pictl.server.repository

import io.ktor.application.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.cometd.bayeux.Channel
import org.cometd.bayeux.Message
import org.cometd.bayeux.client.ClientSession
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.client.BayeuxClient
import org.cometd.client.http.okhttp.OkHttpClientTransport
import org.cometd.client.transport.HttpClientTransport
import org.cometd.client.websocket.okhttp.OkHttpWebSocketTransport
import org.kodein.di.DI
import org.kodein.di.instance
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.regex.Pattern

class SlimboxCometdRepository(di: DI) {

    private val application: Application by di.instance()

    /** The maximum number of milliseconds to wait before considering a request to the LMS failed  */
    private val LONG_POLLING_TIMEOUT = 120000

    /** [java.util.regex.Pattern] that splits strings on forward slash.  */
    private val mSlashSplitPattern: Pattern = Pattern.compile("/")

    /** The channel to publish one-shot requests to.  */
    private val CHANNEL_SLIM_REQUEST = "/slim/request"

    /** The format string for the channel to listen to for responses to one-shot requests.  */
    private val CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT = "/%s/slim/request/%s"

    /** The channel to publish subscription requests to.  */
    private val CHANNEL_SLIM_SUBSCRIBE = "/slim/subscribe"

    /** The channel to publish unsubscribe requests to.  */
    private val CHANNEL_SLIM_UNSUBSCRIBE = "/slim/unsubscribe"

    /** The format string for the channel to listen to for server status events.  */
    private val CHANNEL_SERVER_STATUS_FORMAT = "/%s/slim/serverstatus"

    /** The format string for the channel to listen to for player status events.  */
    private val CHANNEL_PLAYER_STATUS_FORMAT = "/%s/slim/playerstatus/%s"

    /** The format string for the channel to listen to for display status events.  */
    private val CHANNEL_DISPLAY_STATUS_FORMAT = "/%s/slim/displaystatus/%s"

    /** The format string for the channel to listen to for menu status events.  */
    private val CHANNEL_MENU_STATUS_FORMAT = "/%s/slim/menustatus/%s"

    // Maximum time for wait replies for server capabilities
    private val HANDSHAKE_TIMEOUT: Long = 4000

    fun play() {
        val logging = HttpLoggingInterceptor(MyOkHttpLogger())
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val httpClient = OkHttpClient.Builder().addNetworkInterceptor(logging).build()
        val wsTransport = OkHttpWebSocketTransport(null, httpClient)

        val options = mutableMapOf<String, Any>()
        options[HttpClientTransport.MAX_NETWORK_DELAY_OPTION] = LONG_POLLING_TIMEOUT
        val httpTransport = OkHttpClientTransport(options, httpClient)

        val bayeuxClient = BayeuxClient("http://localhost:9000/cometd", httpTransport)

//        bayeuxClient.addExtension(AckExtension())
//        bayeuxClient.addExtension(SqueezeboxBayeuxExtension())

        bayeuxClient.getChannel(Channel.META_HANDSHAKE)
            .addListener(ClientSessionChannel.MessageListener { channel, message ->
                if (message.isSuccessful) {
                    establishSubscriptions(bayeuxClient)
                } else {
                    application.log.error("Could not establish HANDSHAKE: $message")
                }
            })
        bayeuxClient.getChannel(Channel.META_CONNECT)
            .addListener(ClientSessionChannel.MessageListener { channel, message ->
                application.log.info("Something happen on the META_CONNECT channel: $message")
            })

        bayeuxClient.handshake()
    }

    private fun establishSubscriptions(bayeuxClient: BayeuxClient) {
        application.log.info("establish subscriptions..")
        val clientId = bayeuxClient.id

        bayeuxClient.getChannel(String.format(CHANNEL_SLIM_REQUEST_RESPONSE_FORMAT, clientId, "*"))
            .subscribe { channel, message ->
                application.log.info("Message on '$channel': $message")
            }

        bayeuxClient.getChannel(String.format(CHANNEL_SERVER_STATUS_FORMAT, clientId)).subscribe { channel, message ->
            application.log.info("Message on '$channel': $message")
        }
        bayeuxClient.getChannel(String.format(CHANNEL_PLAYER_STATUS_FORMAT, clientId, "*"))
            .subscribe { channel, message ->
                application.log.info("Message on '$channel': $message")
            }
        bayeuxClient.getChannel(CHANNEL_SLIM_SUBSCRIBE).subscribe { channel, message ->
            application.log.info("Message on '$channel': $message")
        }
        bayeuxClient.getChannel(String.format(CHANNEL_DISPLAY_STATUS_FORMAT, clientId, "*"))
            .subscribe { channel, message ->
                application.log.info("Message on '$channel': $message")
            }
        bayeuxClient.getChannel(String.format(CHANNEL_MENU_STATUS_FORMAT, clientId, "*"))
            .subscribe { channel, message ->
                application.log.info("Message on '$channel': $message")
            }

        val serverStatusReq = mapOf(
            "request" to listOf(
                "",
                listOf(
                    "serverstatus",
                    "0",
                    "255",
                    "playerprefs:playtrackalbum,defeatDestructiveTouchToPlay",
                    "prefs:mediadirs, defeatDestructiveTouchToPlay"
                )
            ),
            "response" to "/${bayeuxClient.id}/slim/serverstatus"
        )
        bayeuxClient
            .getChannel(CHANNEL_SLIM_REQUEST)
            .publish(serverStatusReq) { application.log.info("Message from REQUEST: $it") }
        val menuStatusReq = mapOf(
            "request" to listOf("b8:27:eb:44:2f:38", listOf("menustatus")),
            "response" to "/${bayeuxClient.id}/slim/menustatus/mystatus"
        )
        bayeuxClient
            .getChannel(CHANNEL_SLIM_REQUEST)
            .publish(menuStatusReq) { application.log.info("Message from REQUEST: $it") }
        val playerStatusReq = mapOf(
            "request" to listOf(
                "b8:27:eb:44:2f:38",
                listOf("status", "-", "1", "useContextMenu:1", "subscribe:600", "menu:menu")
            ),
            "response" to "/${bayeuxClient.id}/slim/playerstatus/myplayer"
        )
        bayeuxClient
            .getChannel(CHANNEL_SLIM_SUBSCRIBE)
            .publish(playerStatusReq) { application.log.info("Message from SUBSCRIBE: $it") }
        val displayStatusReq = mapOf(
            "request" to listOf("b8:27:eb:44:2f:38", listOf("displaystatus", "subscribe:showbriefly")),
            "response" to "/${bayeuxClient.id}/slim/displaystatus/mydisplay"
        )
        bayeuxClient
            .getChannel(CHANNEL_SLIM_SUBSCRIBE)
            .publish(displayStatusReq) { application.log.info("Message from SUBSCRIBE: $it") }
    }

    class SqueezeboxBayeuxExtension : ClientSession.Extension {
        val myUuid = "f79a6d51c5274e4c8d98ca6d4fb32818" // UUID.randomUUID()
        val localhost = InetAddress.getLocalHost()
        val ni = NetworkInterface.getByInetAddress(localhost)
        override fun sendMeta(session: ClientSession?, message: Message.Mutable?): Boolean {
            // mysqueezebox.com requires an ext field in the handshake message
            if (Channel.META_HANDSHAKE == message?.channel) {
                val ext = mapOf("uuid" to myUuid.toString(), "rev" to "2.2.1", "mac" to "f4:0f:24:35:da:b0")
                message[Message.EXT_FIELD] = ext;
            }
            return true
        }

        private fun convertMacAddress(): String {
            val myMac = ni.getHardwareAddress()

            val hexadecimal = mutableListOf<String>()
            myMac.forEach {
                hexadecimal.add(String.format("%20X", it))
            }
            return hexadecimal.joinToString(separator = ":")
        }
    }

    class MyOkHttpLogger : HttpLoggingInterceptor.Logger {
        private val logger = LoggerFactory.getLogger(MyOkHttpLogger::class.java)
        override fun log(message: String) {
            logger.info(message)
        }

    }


}