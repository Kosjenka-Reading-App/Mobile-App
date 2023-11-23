package com.dsd.kosjenka.data.remote

import com.dsd.kosjenka.utils.error.ErrorManager
import retrofit2.Response

open class BaseRemote(private val errorManager: ErrorManager) {

    protected suspend fun <T> parseResult(networkCall: suspend () -> Response<T>): T? {
        val response = networkCall.invoke()
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            errorManager.getAppError(errorBody)
        }
        return response.body()
    }

}