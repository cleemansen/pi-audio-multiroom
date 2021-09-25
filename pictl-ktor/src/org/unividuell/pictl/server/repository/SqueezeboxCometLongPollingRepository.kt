package org.unividuell.pictl.server.repository

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import org.cometd.client.BayeuxClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.unividuell.pictl.server.repository.cometd.model.SlimCometRequest

/*
NOTES:
- get Favorites: [{"clientId":"9ddb7286","data":{"request":["24:05:0f:95:46:70",["favorites","items","0","50","menu:favorites","useContextMenu:1"]],"response":"/9ddb7286/slim/request/1"},"channel":"/slim/request","id":"34"}]
- slimserver web-ui: {"id":1,"method":"slim.request","params":["b8:27:eb:44:2f:38",["status","-",1,"tags:cgABbehldiqtyrSuoKLNJ"]]}
 */

abstract class SqueezeboxCometLongPollingRepository : KoinComponent {

    protected val application: Application by inject()

    protected val slimserverHost =
        application.environment.config.property("ktor.application.slimserver.host").getString()

    protected val bayeuxClient: BayeuxClient by inject()

    protected val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    /**
     * https://github.com/Logitech/slimserver/blob/public/8.3/Slim/Web/Cometd.pm#L512
     *
     * A valid /slim/request message looks like this:
     * {
     *   channel  => '/slim/request',
     *   id       => <unique id>, (optional)
     *   data     => {
     *     response => '/slim/<clientId>/request',
     *     request  => [ '', [ 'menu', 0, 100, ],
     *     priority => <value>, # optional priority value, is passed-through with the response
     *   }
     * }
     */
    protected fun slimRequestData(
        playerId: String,
        command: List<String>
    ): SlimCometRequest {
        return SlimCometRequest(
            request = listOf(playerId, command),
            response = SqueezeboxCometSubscriptionRepository.Channels.slimRequest.toString()
        )
    }

}