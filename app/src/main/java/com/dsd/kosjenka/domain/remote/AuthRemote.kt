package com.dsd.kosjenka.domain.remote

import com.dsd.kosjenka.data.remote.ApiService
import com.dsd.kosjenka.data.remote.BaseRemote
import com.dsd.kosjenka.domain.models.User
import com.dsd.kosjenka.domain.request_objects.ForgotPasswordRequest
import com.dsd.kosjenka.domain.request_objects.ResetPasswordRequest
import com.dsd.kosjenka.utils.error.ErrorManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRemote @Inject constructor(
    private val apiService: ApiService,
    errorManager: ErrorManager,
) : BaseRemote(errorManager) {

    suspend fun register(
        email: String,
        password: String,
    ) = parseResult {
        apiService.register(
            user = User(email, password)
        )
    }

    suspend fun login(email: String, password: String, token: String? = null) = parseResult {
        apiService.login(
            user = User(email = email, password = password)
        )
    }

    suspend fun reset(
        token: String,
        newPassword: String,
    ) = parseResult {
        apiService.resetPassword(
            request = ResetPasswordRequest(token = token, password = newPassword)
        )
    }

    suspend fun forgotPassword(
        email:String
    )=parseResult{
        apiService.passwordForgot(
            request = ForgotPasswordRequest(email=email)
        )
    }


}