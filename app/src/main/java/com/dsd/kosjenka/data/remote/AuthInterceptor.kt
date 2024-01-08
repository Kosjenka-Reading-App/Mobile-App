package com.dsd.kosjenka.data.remote

import android.util.Log
import com.dsd.kosjenka.domain.repository.AuthRepository
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
): Interceptor {

    private val TAG: String = "AuthInterceptor"
    companion object {
        private const val AUTH_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val HTTP_STATUS_CODE_UNAUTHORIZED = 403
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        Log.d(TAG, "Request intercepted")
        val token = runBlocking {
            tokenManager.accessToken
        }

        val originalRequest: Request = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // If token has been saved, add it to the request
        requestBuilder.addHeader(AUTH_HEADER, BEARER_PREFIX + token)

        val request = requestBuilder.build()
        return chain.proceed(request)

        // Check for 403 response indicating token expiration
//        if (response.code == HTTP_STATUS_CODE_UNAUTHORIZED) {
//            synchronized(this) {
//                runBlocking {
//                    Log.d(TAG, "Trying to refresh token")
//                    // Refresh the token using the refresh token
//                    val refreshResponse = authRepository.get().refreshToken(sharedPreferences.refreshToken)
//                    refreshResponse.let {
//                        sharedPreferences.apply {
//                            refreshToken = it!!.refresh_token
//                            accessToken = it.access_token
//                        }
//                    }
//                }
//                val newRequest: Request = originalRequest.newBuilder()
//                    .header(AUTH_HEADER, BEARER_PREFIX + sharedPreferences.accessToken)
//                    .build()
//                Log.d(TAG, response.code.toString())
//                return chain.proceed(newRequest)
//            }
//        }
//        return response
    }
}