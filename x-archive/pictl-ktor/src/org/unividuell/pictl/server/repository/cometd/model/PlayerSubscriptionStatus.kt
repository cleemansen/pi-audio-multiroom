package org.unividuell.pictl.server.repository.cometd.model

import java.time.Instant

data class PlayerSubscriptionStatus(
    val bayeuxClientId: String,
    val playerSubscriptions: MutableList<PlayerSubscription> = mutableListOf()
) {
    data class PlayerSubscription(
        val playerId: String,
        val since: Instant = Instant.now()
    )
}