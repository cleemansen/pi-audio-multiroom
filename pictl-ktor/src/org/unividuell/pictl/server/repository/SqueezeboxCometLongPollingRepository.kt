package org.unividuell.pictl.server.repository

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import org.cometd.bayeux.ChannelId
import org.cometd.client.BayeuxClient
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.controller.model.PlayerStatusViewModel
import org.unividuell.pictl.server.repository.cometd.model.PlayerCometdResponse
import org.unividuell.pictl.server.repository.cometd.model.PlayersCometResponse
import org.unividuell.pictl.server.repository.cometd.model.SlimCometRequest
import org.unividuell.pictl.server.repository.cometd.model.SlimUnsubscribeCometRequest
import org.unividuell.pictl.server.usecase.SubscribeForPlayersUpdatesInteractor
import org.unividuell.pictl.server.usecase.TogglePlayPausePlayerInteractor
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.Charset
import java.time.Duration
import kotlin.concurrent.fixedRateTimer

/*
NOTES:
- get Favorites: [{"clientId":"9ddb7286","data":{"request":["24:05:0f:95:46:70",["favorites","items","0","50","menu:favorites","useContextMenu:1"]],"response":"/9ddb7286/slim/request/1"},"channel":"/slim/request","id":"34"}]
 */

class SqueezeboxCometLongPollingRepository(
    di: DI
) : SubscribeForPlayersUpdatesInteractor.DataSource,
    TogglePlayPausePlayerInteractor.DataSource {

    private val application: Application by di.instance()

    private val slimserverHost = application.environment.config.property("ktor.application.slimserver.host").getString()

    private val bayeuxClient: BayeuxClient by di.instance()

    private val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    class Channels {
        companion object {
            val slimSubscribe = ChannelId("/slim/subscribe")
            val slimUnsubscribe = ChannelId("/slim/unsubscribe")
            val slimRequest = ChannelId("/slim/request")
            val activeDynamicChannels = mutableMapOf<ChannelId, SlimCometRequest>()
        }
    }

    init {
        fixedRateTimer(
            name = "player-subscription-monitor",
            period = Duration.ofSeconds(30).toMillis()
        ) {
            application.log.info(Channels.activeDynamicChannels.keys.toString())
        }
    }

    companion object {
        val PlayerEvent: EventDefinition<PlayerStatusViewModel> = EventDefinition()
    }

    override fun unsubscribe() {
        // squeezebox will handle unsubscribe from all active subscriptions during /meta/disconnect
        // so use this clean-up feature for now..
        disconnect()
    }

    override fun disconnect() {
        bayeuxClient.disconnect {
            application.log.info("Server precessed the disconnect request.")
        }
        bayeuxClient.waitFor(10_000, BayeuxClient.State.DISCONNECTED)
        Channels.activeDynamicChannels.clear()
        application.log.info("Disconnected from CometD..")

    }

    override fun connectAndSubscribe() {
        if (bayeuxClient.isDisconnected) {
            bayeuxClient.handshake()
            val handshake = bayeuxClient.waitFor(1_000, BayeuxClient.State.CONNECTED)
            if (handshake) {
                establishSubscriptions(bayeuxClient)
            }
        } else {
            establishSubscriptions(bayeuxClient)
        }
    }

    override fun requestUpdate() {
        Channels.activeDynamicChannels.forEach {
            if (bayeuxClient.isConnected) {
                bayeuxClient.getChannel(Channels.slimRequest)
                    .publish(it.value)
            }
        }
    }


    private fun establishSubscriptions(bayeuxClient: BayeuxClient) {
        application.log.info("[${bayeuxClient.id}] establishing subscriptions..")

        bayeuxClient.getChannel(Channels.slimRequest).subscribe { channel, message ->
            application.log.info("${channel.id} -> $message")
        }

        if (!Channels.activeDynamicChannels.keys.contains(playersStatusChannel(bayeuxClient = bayeuxClient))) {
            subscribeForPlayers(bayeuxClient)
        }
    }

    private fun playersStatusChannel(bayeuxClient: BayeuxClient) = ChannelId("/${bayeuxClient.id}/pictl/players")

    private fun subscribeForPlayers(bayeuxClient: BayeuxClient) {
        val channelId = playersStatusChannel(bayeuxClient = bayeuxClient)
        val playersSubscriptionRequest = slimSubscriptionRequestData(
            responseChannel = channelId.toString(),
            playerId = "",
            command = "players",
            args = emptyList()
        )
        bayeuxClient.getChannel(channelId).subscribe { channel, message ->
//            application.log.info("received on ${channel.channelId}: ${objectMapper.writeValueAsString(message.dataAsMap)}")
            Channels.activeDynamicChannels[channel.channelId] = playersSubscriptionRequest
            val actual = mapPlayersResponse(message.dataAsMap)
            application.log.info("[${bayeuxClient.id}] " + actual.toString())
            actual?.players?.forEach { player ->
                if (!Channels.activeDynamicChannels.keys.contains(
                        playerStatusChannel(
                            bayeuxClient = bayeuxClient,
                            playerId = player.playerId
                        )
                    )
                ) {
                    subscribeForPlayerStatus(bayeuxClient = bayeuxClient, playerId = player.playerId)
                }
                if (!Channels.activeDynamicChannels.keys.contains(
                        playerModeChannel(
                            bayeuxClient = bayeuxClient,
                            playerId = player.playerId
                        )
                    )
                ) {
                    subscribeForPlayerMode(bayeuxClient = bayeuxClient, playerId = player.playerId)
                }
            }
        }
        bayeuxClient
            .getChannel(Channels.slimSubscribe)
            .publish(playersSubscriptionRequest) {
                application.log.debug("I REQUESTED the playerstatus: $it")
            }
    }

    private fun playerStatusChannel(bayeuxClient: BayeuxClient, playerId: String) =
        ChannelId("/${bayeuxClient.id}/pictl/player/${playerId.replace(oldChar = ':', newChar = '-')}")

    private fun playerModeChannel(bayeuxClient: BayeuxClient, playerId: String) =
        ChannelId("/${bayeuxClient.id}/pictl/player/${playerId.replace(oldChar = ':', newChar = '-')}/mode")

    private fun subscribeForPlayerStatus(bayeuxClient: BayeuxClient, playerId: String) {
        val channelId = playerStatusChannel(bayeuxClient = bayeuxClient, playerId = playerId)
        val playerStatusSubscriptionRequest = slimSubscriptionRequestData(
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
            // T: samplerate Song sample rate (in KHz)
            // r: bitrate
            // u: Song file url.
            args = listOf("tags:galKLmNrLT")
        )
        bayeuxClient.getChannel(channelId).subscribe { channel, message ->
//            application.log.info("received on ${channel.channelId}: ${objectMapper.writeValueAsString(message.dataAsMap)}")
            Channels.activeDynamicChannels[channel.channelId] = playerStatusSubscriptionRequest
            val actual = mapPlayerResponse(message.dataAsMap)
            application.log.info("[${bayeuxClient.id}] " + actual.toString())
            raisePlayerStatusUpdateEvent(channelId, actual)
        }

        bayeuxClient
            .getChannel(Channels.slimSubscribe)
            .publish(playerStatusSubscriptionRequest) { application.log.debug("I REQUESTED the playerstatus: $it") }
    }

    private fun subscribeForPlayerMode(bayeuxClient: BayeuxClient, playerId: String) {
        val channelId = playerModeChannel(bayeuxClient = bayeuxClient, playerId = playerId)
        val playerModeSubscriptionRequest = slimSubscriptionRequestData(
            responseChannel = channelId.toString(),
            playerId = playerId,
            command = "mode ?",
            args = emptyList()
        )
        bayeuxClient.getChannel(channelId).subscribe { channel, message ->
            Channels.activeDynamicChannels[channel.channelId] = playerModeSubscriptionRequest
            val actual = message.dataAsMap
            application.log.info("[${bayeuxClient.id}] " + actual)
        }
        bayeuxClient
            .getChannel(Channels.slimSubscribe)
            .publish(playerModeSubscriptionRequest) { application.log.info("I REQUESTED the playermode: $it") }
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
        args: List<String>,
        isSubscription: Boolean = true
    ): SlimCometRequest {
        // If args doesn't contain a 'subscribe' key, treat it as a normal subscribe call and not a request + subscribe
        // [https://github.com/Logitech/slimserver/blob/b7d9ed8e7356981cb9d5ce2cea67bd5f1d7b6ee3/Slim/Web/Cometd.pm#L766]
        val confirmedArgs = if (isSubscription && !args.any { it.startsWith(prefix = "subscribe") }) {
            args.toMutableList().apply {
                // The number indicates the time interval in seconds between automatic generations
                // in case nothing happened to the player in the interval.
                add("subscribe:60")
            }.toList()
        } else {
            args
        }
        val query = mutableListOf(command, start, itemsPerRequest)
        query.addAll(confirmedArgs)
        val request = listOf(playerId, query)
        return SlimCometRequest(
            response = responseChannel,
            request = request
        )
    }

    /**
     * https://github.com/cleemansen/slimserver/blob/public%2F8.0/Slim/Web/Cometd.pm#L478
     *
     * # A valid /slim/unsubscribe message looks like this:
     * {
     *   channel  => '/slim/unsubscribe',
     *   data     => {
     *     unsubscribe => '/slim/serverstatus',
     *   }
     * }
     */
    private fun slimUnsubscribeRequestData(
        unsubscribeChannel: String
    ): SlimUnsubscribeCometRequest = SlimUnsubscribeCometRequest(unsubscribe = unsubscribeChannel)

    private fun raisePlayerStatusUpdateEvent(
        channelId: ChannelId,
        actual: PlayerCometdResponse
    ) {
        val playerId = channelId.getSegment(channelId.depth() - 1).replace(oldChar = '-', newChar = ':')
        val cleanedArtworkUrl = try {
            URLDecoder.decode(actual.remoteMeta?.artworkUrl, Charset.defaultCharset())
        } catch (e: UnsupportedEncodingException) {
            actual.remoteMeta?.artworkUrl
        }.let {
            if (it?.startsWith("/imageproxy/") == true || it?.startsWith("/plugins/") == true) {
                slimserverHost + it
            } else {
                it
            }
        }
        application.environment.monitor.raise(
            PlayerEvent,
            PlayerStatusViewModel(
                playerId = playerId,
                playerName = actual.playerName,
                title = actual.remoteMeta?.title,
                artist = actual.remoteMeta?.artist,
                remoteTitle = actual.remoteMeta?.remoteTitle,
                artworkUrl = cleanedArtworkUrl,
                mode = actual.mode,
                syncController = actual.syncMaster,
                syncNodes = actual.syncSlaves?.split(',') ?: emptyList()
            )
        )
    }

    private fun mapPlayersResponse(data: Map<String, Any>): PlayersCometResponse? {
        if (data["count"] == 0) {
            application.log.warn("no players available!")
            return null
        }
        return (data["players_loop"] as List<Any>).map {
            objectMapper.convertValue<PlayersCometResponse.Player>(it)
        }.let {
            PlayersCometResponse(players = it)
        }
    }

    private fun mapPlayerResponse(data: Map<String, Any>): PlayerCometdResponse {
        return objectMapper.convertValue<PlayerCometdResponse>(data)
    }

    override fun togglePlayPausePlayer(playerId: String) {
        bayeuxClient.getChannel(Channels.slimRequest)
            .publish(
                slimRequestData(
                    playerId = playerId,
                    command = listOf("pause")
                )
            )
    }

    private fun slimRequestData(
        playerId: String,
        command: List<String>
    ): SlimCometRequest {
        return SlimCometRequest(
            request = listOf(playerId, command),
            response = Channels.slimRequest.toString()
        )
    }

}