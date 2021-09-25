package org.unividuell.pictl.server.repository

import com.fasterxml.jackson.module.kotlin.convertValue
import io.ktor.application.*
import io.micrometer.core.instrument.Counter
import io.micrometer.prometheus.PrometheusMeterRegistry
import mu.KotlinLogging
import org.cometd.bayeux.ChannelId
import org.cometd.client.BayeuxClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.unividuell.pictl.server.controller.model.PlayerStatusViewModel
import org.unividuell.pictl.server.repository.cometd.model.PlayerCometdResponse
import org.unividuell.pictl.server.repository.cometd.model.ServerstatusCometResponse
import org.unividuell.pictl.server.repository.cometd.model.SlimCometRequest
import org.unividuell.pictl.server.repository.cometd.model.SlimUnsubscribeCometRequest
import org.unividuell.pictl.server.usecase.RequestPlayersUpdatesInteractor
import org.unividuell.pictl.server.usecase.SubscribeForPlayersUpdatesInteractor
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.Charset

class SqueezeboxCometSubscriptionRepository :
    KoinComponent,
    SqueezeboxCometLongPollingRepository(),
    SubscribeForPlayersUpdatesInteractor.DataSource,
    RequestPlayersUpdatesInteractor.DataSource {

    private val registry: PrometheusMeterRegistry by inject()

    private val logger = KotlinLogging.logger { }

    private val playerStatusCounter = Counter.builder("player.status")
        .description("a player status event")
        .register(registry)

    companion object {
        val PlayerEvent: EventDefinition<PlayerStatusViewModel> = EventDefinition()
    }

    class Channels {
        companion object {
            /**
             * https://github.com/Logitech/slimserver/blob/public/8.3/Slim/Web/Cometd.pm#L375
             * A request to execute & subscribe to some Logitech Media Server event
             */
            val slimSubscribe = ChannelId("/slim/subscribe")
            val slimUnsubscribe = ChannelId("/slim/unsubscribe")

            /**
             * https://github.com/Logitech/slimserver/blob/public/8.3/Slim/Web/Cometd.pm#L512
             * A request to execute a one-time Logitech Media Server event
             */
            val slimRequest = ChannelId("/slim/request")
            val activeDynamicChannels = mutableMapOf<ChannelId, SlimCometRequest>()
        }
    }

    override fun unsubscribe() {
        // squeezebox will handle unsubscribe from all active subscriptions during /meta/disconnect
        // so use this clean-up feature for now..
        disconnect()
    }

    override fun disconnect() {
        bayeuxClient.disconnect {
            logger.info("Server precessed the disconnect request.")
        }
        bayeuxClient.waitFor(10_000, BayeuxClient.State.DISCONNECTED)
        Channels.activeDynamicChannels.clear()
        logger.info("Disconnected from CometD..")
    }

    override fun connectAndSubscribe() {
        if (bayeuxClient.isDisconnected) {
            bayeuxClient.handshake()
            val handshake = bayeuxClient.waitFor(4_000, BayeuxClient.State.CONNECTED)
            if (handshake) {
                establishSubscriptions(bayeuxClient)
            }
        } else {
            establishSubscriptions(bayeuxClient)
        }
    }

    override fun requestUpdate() {
        Channels.activeDynamicChannels.forEach {
            if (bayeuxClient.isDisconnected) {
                connectAndSubscribe()
            } else {
                bayeuxClient.getChannel(Channels.slimRequest)
                    .publish(it.value)
            }
        }
    }

    private fun establishSubscriptions(bayeuxClient: BayeuxClient) {
        logger.info("[${bayeuxClient.id}] establishing subscriptions..")

        bayeuxClient.getChannel(Channels.slimRequest).subscribe { channel, message ->
            logger.info("${channel.id} -> $message")
            logger.warn { "I'm ignoring this message! ${channel.id} -> $message" }
        }

        if (!Channels.activeDynamicChannels.keys.contains(serverstatusChannel(bayeuxClient = bayeuxClient))) {
            subscribeForServerstatus(bayeuxClient)
        }
    }

    private fun serverstatusChannel(bayeuxClient: BayeuxClient) = ChannelId("/${bayeuxClient.id}/pictl/serverstatus")

    private fun subscribeForServerstatus(bayeuxClient: BayeuxClient) {
        val channelId = serverstatusChannel(bayeuxClient = bayeuxClient)
        val serverstatusSubscriptionRequest = slimSubscriptionRequestData(
            responseChannel = channelId.toString(),
            playerId = "",
            command = "serverstatus",
            args = emptyList()
        )
        bayeuxClient.getChannel(channelId).subscribe { channel, message ->
//            logger.info("received on ${channel.channelId}: ${objectMapper.writeValueAsString(message.dataAsMap)}")
            Channels.activeDynamicChannels[channel.channelId] = serverstatusSubscriptionRequest
            val actual = mapServerstatusResponse(message.dataAsMap)
            logger.debug("[${bayeuxClient.id}|${message.id} on ${channel.channelId}] " + actual.toString())
            actual?.players?.filter { it.connected == 1 }?.forEach { player ->
                if (!Channels.activeDynamicChannels.keys.contains(
                        playerStatusChannel(
                            bayeuxClient = bayeuxClient,
                            playerId = player.playerId
                        )
                    )
                ) {
                    subscribeForPlayerStatus(bayeuxClient = bayeuxClient, playerId = player.playerId)
                }
            }
            // TODO: notify about gone player
        }
        bayeuxClient
            .getChannel(Channels.slimSubscribe)
            .publish(serverstatusSubscriptionRequest) {
                logger.info("[${bayeuxClient.id}] I subscribed for $channelId: $it")
            }
    }

    private fun playerStatusChannel(bayeuxClient: BayeuxClient, playerId: String) =
        // We expect the clientId to be part of the response channel
        ChannelId("/${bayeuxClient.id}/pictl/player/${playerId.replace(oldChar = ':', newChar = '-')}")

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
//            logger.info("received on ${channel.channelId}: ${objectMapper.writeValueAsString(message.dataAsMap)}")
            Channels.activeDynamicChannels[channel.channelId] = playerStatusSubscriptionRequest
            val actual = mapPlayerResponse(message.dataAsMap)
            logger.debug("[${bayeuxClient.id}|${message.id} on ${channel.channelId}] " + actual.toString())
            playerStatusCounter.increment()
            raisePlayerStatusUpdateEvent(channelId, actual)
        }

        bayeuxClient
            .getChannel(Channels.slimSubscribe)
            .publish(playerStatusSubscriptionRequest) { logger.info("[${bayeuxClient.id}] I subscribed for $channelId: $it") }
    }

    /**
     * https://github.com/Logitech/slimserver/blob/b7d9ed8e7356981cb9d5ce2cea67bd5f1d7b6ee3/Slim/Web/Cometd.pm#L374
     * A request to execute & subscribe to some Logitech Media Server event
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
     *
     * If the request array doesn't contain 'subscribe:foo' the request will be treated
     * as a normal subscription using Request::subscribe()
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
            if (actual.remoteMeta?.artworkUrl != null) {
                URLDecoder.decode(actual.remoteMeta.artworkUrl, Charset.defaultCharset())
            } else {
                null
            }
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
                mixerVolume = actual.mixerVolume,
                connected = actual.playerConnected?.let { it == 1 },
                ipAddress = actual.playerIp,
                syncController = actual.syncMaster,
                syncNodes = actual.syncSlaves?.split(',') ?: emptyList()
            )
        )
    }

    private fun mapServerstatusResponse(data: Map<String, Any>): ServerstatusCometResponse? {
        if (data["count"] == 0) {
            logger.warn("no players available!")
            return null
        }
        return (data["players_loop"] as List<Any>).map {
            objectMapper.convertValue<ServerstatusCometResponse.Player>(it)
        }.let {
            ServerstatusCometResponse(players = it)
        }
    }

    private fun mapPlayerResponse(data: Map<String, Any>): PlayerCometdResponse {
        return objectMapper.convertValue<PlayerCometdResponse>(data)
    }
}