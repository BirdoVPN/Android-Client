package app.birdo.vpn.data.api

import app.birdo.vpn.data.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that adds auth headers to every request.
 * - User-Agent: Birdo-Android/<version> (Android)
 * - X-Desktop-Client: birdo-android (on POST requests)
 * - Authorization: Bearer <token> (when logged in)
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("User-Agent", "Birdo-Android/${app.birdo.vpn.BuildConfig.APP_VERSION} (Android)")

        // Identify this client on all requests — backend auth guards may check it
        builder.header("X-Desktop-Client", "birdo-android")

        // Add auth token if available — TokenManager is now non-suspend,
        // no runBlocking needed (was blocking OkHttp dispatcher threads).
        val token = tokenManager.getAccessToken()
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(builder.build())
    }
}
