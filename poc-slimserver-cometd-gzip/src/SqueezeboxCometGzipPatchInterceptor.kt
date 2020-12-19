import okhttp3.Interceptor
import okhttp3.Response

/**
 * It seams there is an issue in the gzip-encoding in slimserver (silent fail).
 * So disable this http-client feature here for now.
 * See https://github.com/Logitech/slimserver/issues/481
 */
class SqueezeboxCometGzipPatchInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val noGzip = request.newBuilder().removeHeader("Accept-Encoding").build()
        return chain.proceed(noGzip)
    }

}