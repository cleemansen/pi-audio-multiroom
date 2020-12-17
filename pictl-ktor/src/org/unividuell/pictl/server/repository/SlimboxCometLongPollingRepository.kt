package org.unividuell.pictl.server.repository

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.cometd.bayeux.Channel
import org.cometd.bayeux.ChannelId
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.client.BayeuxClient
import org.cometd.client.http.okhttp.OkHttpClientTransport
import org.cometd.client.transport.HttpClientTransport
import org.cometd.common.JacksonJSONContextClient
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.network.cometd.CometOkHttpLogger
import org.unividuell.pictl.server.network.cometd.SqueezeboxCometConnectPatchInterceptor
import java.time.Instant

class SlimboxCometLongPollingRepository(di: DI) {

    private val application: Application by di.instance()

    private val bayeuxClient = buildBayeuxClient()

    private val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val playerSubscriptionStatus = mutableMapOf<String, PlayerSubscriptionStatus>()

    data class PlayerSubscriptionStatus(
        val bayeuxClientId: String,
        val playerSubscriptions: MutableList<PlayerSubscription> = mutableListOf()
    ) {
        data class PlayerSubscription(
            val playerId: String,
            val since: Instant = Instant.now()
        )
    }

    fun bye() {
        bayeuxClient.disconnect {
            application.log.info("Server precessed the disconnect request.")
        }
        bayeuxClient.waitFor(10_000, BayeuxClient.State.DISCONNECTED)
        application.log.info("Disconnected from CometD..")
    }

    fun play() {
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

    private fun buildBayeuxClient(): BayeuxClient {
        val logging = HttpLoggingInterceptor(CometOkHttpLogger())
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val httpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(logging)
            .addInterceptor(SqueezeboxCometConnectPatchInterceptor())
            .build()

        // The maximum number of milliseconds to wait before considering a request to the LMS failed
        val longPollingTimeout = 30_000
        val options = mutableMapOf<String, Any>()
        options[HttpClientTransport.MAX_NETWORK_DELAY_OPTION] = longPollingTimeout
        val jsonContext = JacksonJSONContextClient()
        options[HttpClientTransport.JSON_CONTEXT_OPTION] = jsonContext
        val httpTransport = OkHttpClientTransport(options, httpClient)

        return BayeuxClient("http://white.local:9000/cometd", httpTransport)
    }

    private fun establishSubscriptions(bayeuxClient: BayeuxClient) {
        application.log.info("establishing subscriptions..")

        subscribeForPlayers(bayeuxClient)
//        subscribeForServerStatus(bayeuxClient)
    }

    private fun subscribeForPlayers(bayeuxClient: BayeuxClient) {
        val channelId = ChannelId("/${bayeuxClient.id}/pictl/players")
        bayeuxClient.getChannel(channelId).subscribe { channel, message ->
//            application.log.info("received on ${channel.channelId}: ${objectMapper.writeValueAsString(message.dataAsMap)}")
            val actual = mapPlayersResponse(message.dataAsMap)
            application.log.info(actual.toString())
            actual.players.forEach { player ->
                if (playerSubscriptionStatus[bayeuxClient.id]?.playerSubscriptions?.any { it.playerId == player.playerId } != true) {
                    subscribeForPlayerStatus(bayeuxClient = bayeuxClient, playerId = player.playerId)
                    val currentState = playerSubscriptionStatus[bayeuxClient.id]
                    if (currentState != null) {
                        playerSubscriptionStatus[bayeuxClient.id]!!.playerSubscriptions
                            .add(PlayerSubscriptionStatus.PlayerSubscription(playerId = player.playerId))
                    } else {
                        playerSubscriptionStatus[bayeuxClient.id] = PlayerSubscriptionStatus(
                            bayeuxClientId = bayeuxClient.id,
                            playerSubscriptions = mutableListOf(PlayerSubscriptionStatus.PlayerSubscription(playerId = player.playerId))
                        )
                    }
                }
            }
        }
        val playersSubscriptionRequest = slimSubscriptionRequestData(
            responseChannel = channelId.toString(),
            playerId = "",
            command = "players",
            args = emptyList()
        )
        bayeuxClient
            .getChannel("/slim/subscribe")
            .publish(playersSubscriptionRequest) { application.log.debug("I REQUESTED the playerstatus: $it") }
    }

    private fun subscribeForPlayerStatus(bayeuxClient: BayeuxClient, playerId: String) {
        val channelId = ChannelId("/${bayeuxClient.id}/pictl/player/${playerId.replace(oldChar = ':', newChar = '-')}")
        bayeuxClient.getChannel(channelId).subscribe { channel, message ->
//            application.log.info("received on ${channel.channelId}: ${objectMapper.writeValueAsString(message.dataAsMap)}")
            val actual = mapPlayerResponse(message.dataAsMap)
            application.log.info(actual.toString())
        }
        val serverStatusSubscriptionRequest = slimSubscriptionRequestData(
            responseChannel = channelId.toString(),
            playerId = playerId,
            command = "status",
            // g: Genre
            // a: Artist
            // l: Album
            // K: artwork_url
            // L:  info_link
            // m: bpm
            // N: Title of the internet radio station.
            // r: bitrate
            args = listOf("tags:galKLmNr")
        )
        bayeuxClient
            .getChannel("/slim/subscribe")
            .publish(serverStatusSubscriptionRequest) { application.log.debug("I REQUESTED the playerstatus: $it") }
    }

    private fun mapPlayersResponse(data: Map<String, Any>): PlayersResponse {
        return (data["players_loop"] as List<Any>).map {
            objectMapper.convertValue<PlayersResponse.Player>(it)
        }.let {
            PlayersResponse(players = it)
        }
    }

    private fun mapPlayerResponse(data: Map<String, Any>): PlayerResponse {
        return objectMapper.convertValue<PlayerResponse>(data)
    }

    data class PlayersResponse(
        val players: List<Player>
    ) {
        data class Player(
            val power: Int,
            val name: String,
            @JsonProperty("playerid")
            val playerId: String
        )
    }

    data class PlayerResponse(
        @JsonProperty("player_name")
        val playerName: String? = null,
        @JsonProperty("sync_master")
        val syncMaster: String? = null,
        @JsonProperty("sync_slaves")
        val syncSlaves: String? = null,
        @JsonProperty("current_title")
        val currentTitle: String? = null,
        val remoteMeta: RemoteMeta? = null,
        val mode: String? = null
    ) {
        data class RemoteMeta(
            val title: String? = null,
            val artist: String? = null,
            @JsonProperty("remote_title")
            val remoteTitle: String? = null,
            @JsonProperty("artwork_url")
            val artworkUrl: String? = null,
            val bitrate: String? = null
        )
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


}