
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.cometd.bayeux.Channel
import org.cometd.bayeux.ChannelId
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.client.BayeuxClient
import org.cometd.client.http.okhttp.OkHttpClientTransport
import org.cometd.client.transport.HttpClientTransport
import org.cometd.common.JacksonJSONContextClient
import org.slf4j.LoggerFactory

/**
 * start with `java -DSLIMSERVER_HOST="http://localhost:9000" -DPLAYER_ID="ab:cd:12:xx:xx:xx" -DGZIP=on -DLOG_LEVEL_HTTP=WARN -jar target/poc-slimserver-cometd-gzip-POC-jar-with-dependencies.jar`
 *
 * POC for https://github.com/Logitech/slimserver/issues/481
 */
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }
    }

    val slimserverHost = environment.config.property("ktor.application.slimserver.host").getString()
    val playerId = environment.config.property("ktor.application.player.id").getString()
    val gzip = environment.config.property("ktor.application.gzip").getString().let { it == "on" }

    environment.log.info("app started with slimserver-host=$slimserverHost, player-id=$playerId, gzip=$gzip")

    val bayeuxClient = buildBayeuxClient(slimserverHost = slimserverHost, gzip = gzip)
    environment.monitor.subscribe(ApplicationStopped) {
        bye(bayeuxClient = bayeuxClient, application = this)
    }
    poc(bayeuxClient = bayeuxClient, application = this, playerId = playerId)
}

fun poc(bayeuxClient: BayeuxClient, application: Application, playerId: String) {
    bayeuxClient.handshake()
    val handshake = bayeuxClient.waitFor(3_000, BayeuxClient.State.CONNECTED)
    if (handshake) {
        subscribeForPlayerStatus(bayeuxClient, playerId, application)
    }
}

private fun subscribeForPlayerStatus(bayeuxClient: BayeuxClient, playerId: String, application: Application) {
    bayeuxClient.waitFor(10_000, BayeuxClient.State.CONNECTED)
    val channelId = ChannelId("/${bayeuxClient.id}/pictl/player/${playerId.replace(oldChar = ':', newChar = '-')}")
    bayeuxClient.getChannel(channelId).subscribe { channel, message ->
        application.log.info("received on ${channel.channelId}: ${jacksonObjectMapper().writeValueAsString(message.dataAsMap)}")
    }
    val serverStatusSubscriptionRequest = slimSubscriptionRequestData(
        responseChannel = channelId.toString(),
        playerId = playerId,
        command = "status",
        args = listOf("tags:galKLmNrLT")
    )
    bayeuxClient
        .getChannel("/slim/subscribe")
        .publish(serverStatusSubscriptionRequest) { application.log.info("I REQUESTED the playerstatus: $it") }
}

/**
 * https://github.com/Logitech/slimserver/blob/b7d9ed8e7356981cb9d5ce2cea67bd5f1d7b6ee3/Slim/Web/Cometd.pm#L374
 *
 * A valid /slim/subscribe message looks like this:
 * {
 *   channel  => '/slim/subscribe',
 *   id       => <unique id>,
 *   data     => {
 *     response => '/slim/serverstatus', # the channel all messages should be sent back on
 *     request  => [ '', [ 'serverstatus', 0, 50, 'subscribe:60' ],
 *     priority => <value>, # optional priority value, is passed-through with the response
 *   }
 * }
 */
// request  => [ '',      [ 'serverstatus', 0,       50,               'subscribe:60' ]
//             [<playerid>] <command>       <start> <itemsPerResponse> <p3>           ... <pN> <LF>
// p3 .. pN: <p3> through <pN> are tagged parameters. Tags consist of a name followed by ":". For example, "artist:Abba".
private fun slimSubscriptionRequestData(
    responseChannel: String,
    playerId: String,
    command: String,
    start: Int = 0,
    itemsPerRequest: Int = 50,
    args: List<String>
): SubscribeRequestData {
    // If args doesn't contain a 'subscribe' key, treat it as a normal subscribe call and not a request + subscribe
    // [https://github.com/Logitech/slimserver/blob/b7d9ed8e7356981cb9d5ce2cea67bd5f1d7b6ee3/Slim/Web/Cometd.pm#L766]
    val confirmedArgs = if (!args.any { it.startsWith(prefix = "subscribe") }) {
        args.toMutableList().apply {
            // The number indicates the time interval in seconds between automatic generations
            // in case nothing happened to the player in the interval.
            add("subscribe:10")
        }.toList()
    } else {
        args
    }
    val query = mutableListOf(command, start, itemsPerRequest)
    query.addAll(confirmedArgs)
    val request = listOf(playerId, query)
    return SubscribeRequestData(
        response = responseChannel,
        request = request
    )
}

data class SubscribeRequestData(
    val response: String,
    val request: List<Any>
)

private fun buildBayeuxClient(slimserverHost: String, gzip: Boolean): BayeuxClient {
    val logging = HttpLoggingInterceptor(CometOkHttpLogger())
    logging.setLevel(HttpLoggingInterceptor.Level.BODY)
    val httpClientBuilder = OkHttpClient.Builder()
        .addInterceptor(SqueezeboxCometConnectPatchInterceptor())
    if (!gzip) {
        httpClientBuilder.addNetworkInterceptor(SqueezeboxCometGzipPatchInterceptor())
    }
    httpClientBuilder.addNetworkInterceptor(logging)
    val httpClient = httpClientBuilder.build()

    // The maximum number of milliseconds to wait before considering a request to the LMS failed
    val longPollingTimeout = 30_000
    val options = mutableMapOf<String, Any>()
    options[HttpClientTransport.MAX_NETWORK_DELAY_OPTION] = longPollingTimeout
    val jsonContext = JacksonJSONContextClient()
    options[HttpClientTransport.JSON_CONTEXT_OPTION] = jsonContext
    val httpTransport = OkHttpClientTransport(options, httpClient)

    return BayeuxClient("$slimserverHost/cometd", httpTransport)
}

fun bye(bayeuxClient: BayeuxClient, application: Application) {
    application.log.info("Graceful shutdown of cometd connection...")
    bayeuxClient.disconnect {
        application.log.info("Server precessed the disconnect request.")
    }
    bayeuxClient.waitFor(10_000, BayeuxClient.State.DISCONNECTED)
    application.log.info("Disconnected from CometD..")
}

class CometOkHttpLogger : HttpLoggingInterceptor.Logger {
    private val logger = LoggerFactory.getLogger(CometOkHttpLogger::class.java)
    override fun log(message: String) {
        logger.info(message)
    }
}

