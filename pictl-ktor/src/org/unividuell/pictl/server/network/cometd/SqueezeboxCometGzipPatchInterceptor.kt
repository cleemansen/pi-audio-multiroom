package org.unividuell.pictl.server.network.cometd

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Use this interceptor to disable the gzipped responses from slimserver.
 * There WAS an issue (see https://github.com/Logitech/slimserver/issues/481),
 * but now it seems to work as expected also with enabled accept-encoding gzip.
 */
@Deprecated(message = "https://github.com/Logitech/slimserver/issues/481")
class SqueezeboxCometGzipPatchInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val noGzip = request.newBuilder().removeHeader("Accept-Encoding").build()
        return chain.proceed(noGzip)
    }

}