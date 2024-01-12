package com.dsd.kosjenka.utils.error

import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorManager @Inject constructor(private val gson: Gson) {

    private val emailUnavailable = "EmailUnavailable"
    private val loginFailed = "LoginFailed"
    private val authFailed = "AuthFailed"
    private val invalidToken = "InvalidToken"
    private val candidateAlreadyAppliedForJob = "CandidateAlreadyAppliedForJob"
    private val userEmailNotVerified = "UserEmailNotVerified"
    private val userEmailAlreadyVerified = "UserEmailAlreadyVerified"
    private val userAlreadyConnected = "UserAlreadyConnected"

    fun getAppError(errorJson: String?, errorCode: Int) {

        val errorResponse = gson.fromJson(errorJson, ErrorResponse::class.java)
        errorResponse?.let {
            it.errorCode?.let { errCode ->
                throw when (errCode) {
                    emailUnavailable -> EmailUnavailableException()
                    loginFailed -> LoginFailedException()
                    authFailed -> AuthFailedException()
                    candidateAlreadyAppliedForJob -> CandidateAlreadyAppliedForJobException()
                    userEmailNotVerified -> UserEmailNotVerified()
                    userEmailAlreadyVerified -> UserEmailAlreadyVerifiedException()
                    userAlreadyConnected -> UserAlreadyConnectedException()
                    else -> UnknownException()
                }
            } ?: throw when (errorCode) {
                401 -> InvalidTokenExcepion()
                else -> UnknownException()
            }
        } ?: throw UnknownErrorException()

    }
}