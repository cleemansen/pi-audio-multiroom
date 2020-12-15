package org.unividuell.pictl.server.controller

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.kodein.di.DI
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.unividuell.pictl.server.usecase.GetCurrentSongInteractor

fun Route.audioRoutes() {

    val getCurrentSongInteractor: GetCurrentSongInteractor by di().instance()

    route("/audio") {
        get("/current") {
            val song = getCurrentSongInteractor.getCurrentSong()
            if (song != null) {
                call.respond(status = HttpStatusCode.OK, message = song)
            } else {
                call.respond(status = HttpStatusCode.NotFound, message = "")
            }
        }
    }
}