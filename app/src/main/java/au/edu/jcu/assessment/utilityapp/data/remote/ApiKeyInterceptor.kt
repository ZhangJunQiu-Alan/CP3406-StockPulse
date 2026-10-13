package au.edu.jcu.assessment.utilityapp.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/** Adds an API key header to every request made through the OkHttp client it is installed on. */
class ApiKeyInterceptor(
    private val headerName: String,
    private val headerValue: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(headerName, headerValue)
            .build()
        return chain.proceed(request)
    }
}
