package org.unividuell.pictl.server.repository

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.usecase.GetCurrentSongInteractor
import org.unividuell.pictl.server.usecase.TogglePlayPausePlayerInteractor

class SqueezeboxJsonRpcRepository(
    di: DI
) : GetCurrentSongInteractor.DataSource,
    TogglePlayPausePlayerInteractor.DataSource {

    private val client: HttpClient by di.instance()

    data class StatusResponse(
        val result: Result
    ) {
        data class Result(
            val current_title: String
        )
    }

    override fun getCurrentSong(): GetCurrentSongInteractor.DataSource.CurrentSong {
        return runBlocking {
            val response = collectSongInformation()
            GetCurrentSongInteractor.DataSource.CurrentSong(
                artist = response.artist,
                title = response.title,
                album = response.album
            )
        }
    }

    data class CurrentSong(
        val artist: String?,
        val title: String?,
        val album: String?
    )

    private suspend fun collectSongInformation(playerId: String = "b8:27:eb:44:2f:38") = coroutineScope<CurrentSong> {
        val artist = async {
            jsonRpcCall<Map<String, Any>>(
                body = JsonRpcRequest(
                    params = listOf(
                        playerId,
                        listOf("artist", "?")
                    )
                )
            )
        }
        val title = async {
            jsonRpcCall<Map<String, Any>>(
                body = JsonRpcRequest(
                    params = listOf(
                        playerId,
                        listOf("title", "?")
                    )
                )
            )
        }
        val album = async {
            jsonRpcCall<Map<String, Any>>(
                body = JsonRpcRequest(
                    params = listOf(
                        playerId,
                        listOf("album", "?")
                    )
                )
            )
        }

        CurrentSong(
            artist = (artist.await()["result"] as Map<String, String>)["_artist"],
            title = (title.await()["result"] as Map<String, String>)["_title"],
            album = (album.await()["result"] as Map<String, String>)["_album"]
        )
    }

    data class JsonRpcRequest(
        val method: String = "slim.request",
        val params: List<Any>
    )

    private suspend inline fun <reified RESPONSE> jsonRpcCall(body: JsonRpcRequest): RESPONSE {
        val requestBuilder = HttpRequestBuilder()
        requestBuilder.body = body
        requestBuilder.url("http://white.local:9000/jsonrpc.js")
        requestBuilder.method = HttpMethod.Post
        requestBuilder.contentType(ContentType.Application.Json)
        return client.post(requestBuilder)
    }

    override fun togglePlayPausePlayer(playerId: String) {
        runBlocking {
            jsonRpcCall<Map<String, Any>>(
                body = JsonRpcRequest(
                    params = listOf(
                        playerId,
                        listOf("pause")
                    )
                )
            )
        }
    }

}