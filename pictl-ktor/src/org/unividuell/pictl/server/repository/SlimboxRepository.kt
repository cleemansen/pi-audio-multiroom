package org.unividuell.pictl.server.repository

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.instance
import org.unividuell.pictl.server.usecase.GetCurrentSongInteractor

class SlimboxRepository(di: DI) : GetCurrentSongInteractor.DataSource {

    private val client: HttpClient by di.instance()

    data class JsonRpcResponse(
        val result: Result
    ) {
        data class Result(
            val current_title: String
        )
    }

    data class JsonRpcRequest(
        val method: String = "slim.request",
        val params: List<Any>
    )

    override fun getCurrentSong(): GetCurrentSongInteractor.DataSource.CurrentSong {
        return runBlocking {
            val response = client.post<JsonRpcResponse> {
                url("http://white.local:9000/jsonrpc.js")
                contentType(ContentType.Application.Json)
                body = JsonRpcRequest(params = listOf("b8:27:eb:44:2f:38", listOf("status", "-")))
            }
            GetCurrentSongInteractor.DataSource.CurrentSong(title = response.result.current_title)
        }
    }
}