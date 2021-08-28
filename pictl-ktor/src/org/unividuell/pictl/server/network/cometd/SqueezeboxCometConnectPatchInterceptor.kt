package org.unividuell.pictl.server.network.cometd

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * See https://github.com/Logitech/slimserver/issues/478
 *
 * The CometD-client expects the ID send in the request
 * in the received response of the CONNECT process (/meta/connect).
 * org.cometd.client.BayeuxClient.SessionState.matchMetaConnect
 * - https://docs.cometd.org/current/reference/#_bayeux_meta_channels
 * - https://docs.cometd.org/current/reference/#_bayeux_meta_connect
 *
 * But squeezebox does not provide this ID in its response.
 * https://github.com/Logitech/slimserver/blob/public/8.0/Slim/Web/Cometd.pm#L267
 */
class SqueezeboxCometConnectPatchInterceptor : Interceptor {

    private val logger = KotlinLogging.logger { }
    private val mapper = jacksonObjectMapper()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var id: String? = null
        if (isMetaConnect(pathSegments = request.url.pathSegments)) {
            val buffer = Buffer()
            request.body?.writeTo(buffer)

            val contentType = request.body!!.contentType()
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

            // [{"clientId":"50c7ad3d","advice":{"timeout":0},"channel":"/meta/connect","id":"8","connectionType":"long-polling"}]
            val requestBody = mapper.readValue<List<Map<String, Any>>>(buffer.readString(charset))
            id = requestBody[0]["id"] as String
        }

        val response = chain.proceed(request)

        if (isMetaConnect(pathSegments = request.url.pathSegments)) {
            val contentLength = response.body!!.contentLength()
            val source = response.body!!.source()
            source.request(Long.MAX_VALUE) // Buffer the entire body.
            val buffer = source.buffer

            val contentType = response.body!!.contentType()
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

            if (contentLength != 0L) {
                val responseBodyMapped = mapper.readValue<List<Map<String, Any>>>(buffer.clone().readString(charset))
                // [{"channel":"/meta/connect","advice":{"interval":0},"successful":true,"timestamp":"Wed, 16 Dec 2020 18:35:43 GMT","clientId":"d8a45836"}]
                val contentList = responseBodyMapped.toMutableList()
                val content = contentList[0].toMutableMap()
                // add the missing ID
                content["id"] = id!!
                contentList[0] = content
                logger.debug { "I fixed the cometd/connect response by injecting the request ID '$id' into the response." }
                return response.newBuilder().body(
                    mapper.writeValueAsString(contentList).toResponseBody()
                ).build()
            }
        }

        return response
    }

    private fun isMetaConnect(pathSegments: List<String>): Boolean =
        pathSegments.joinToString(separator = "/") == "cometd/connect"

}