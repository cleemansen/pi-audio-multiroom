package org.unividuell.pictl.server.repository

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import org.cometd.bayeux.Channel
import org.cometd.bayeux.ChannelId
import org.cometd.bayeux.client.ClientSessionChannel
import org.cometd.client.BayeuxClient
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.controller.model.PlayerStatusViewModel
import org.unividuell.pictl.server.repository.cometd.model.PlayerCometdResponse
import org.unividuell.pictl.server.repository.cometd.model.PlayerSubscriptionStatus
import org.unividuell.pictl.server.repository.cometd.model.PlayersCometResponse
import org.unividuell.pictl.server.repository.cometd.model.SubscribeCometRequest
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.Charset

/*
NOTES:
- get Favorites: [{"clientId":"9ddb7286","data":{"request":["24:05:0f:95:46:70",["favorites","items","0","50","menu:favorites","useContextMenu:1"]],"response":"/9ddb7286/slim/request/1"},"channel":"/slim/request","id":"34"}]
 */

class SqueezeboxCometLongPollingRepository(di: DI) {

    private val application: Application by di.instance()

    private val slimserverHost = application.environment.config.property("ktor.application.slimserver.host").getString()

    private val bayeuxClient: BayeuxClient by di.instance()

    private val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val playerSubscriptionStatus = mutableMapOf<String, PlayerSubscriptionStatus>()

    companion object {
        val PlayerEvent: EventDefinition<PlayerStatusViewModel> = EventDefinition()
    }

    fun disconnect() {
        bayeuxClient.disconnect {
            application.log.info("Server precessed the disconnect request.")
        }
        bayeuxClient.waitFor(10_000, BayeuxClient.State.DISCONNECTED)
        application.log.info("Disconnected from CometD..")
    }

    fun connectAndSubscribe() {
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

        subscribeForPlayers(bayeuxClient)
//        subscribeForServerStatus(bayeuxClient)
    }

    private fun subscribeForPlayers(bayeuxClient: BayeuxClient) {
        val channelId = ChannelId("/${bayeuxClient.id}/pictl/players")
        bayeuxClient.getChannel(channelId).subscribe { channel, message ->
//            application.log.info("received on ${channel.channelId}: ${objectMapper.writeValueAsString(message.dataAsMap)}")
            val actual = mapPlayersResponse(message.dataAsMap)
            application.log.info(actual.toString())
            actual?.players?.forEach { player ->
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
            raisePlayerStatusUpdateEvent(channelId, actual)
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
            // T: samplerate Song sample rate (in KHz)
            // r: bitrate
            // u: Song file url.
            args = listOf("tags:galKLmNrLT")
        )
        bayeuxClient
            .getChannel("/slim/subscribe")
            .publish(serverStatusSubscriptionRequest) { application.log.debug("I REQUESTED the playerstatus: $it") }
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
    ): SubscribeCometRequest {
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
        return SubscribeCometRequest(
            response = responseChannel,
            request = request
        )
    }

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

}