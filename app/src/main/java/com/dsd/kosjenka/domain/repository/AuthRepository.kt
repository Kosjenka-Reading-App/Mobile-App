package com.dsd.kosjenka.domain.repository

import com.dsd.kosjenka.data.remote.BaseRepository
import com.dsd.kosjenka.domain.remote.AuthRemote
import com.dsd.kosjenka.utils.NetManager
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.error.NoInternetException
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val remote: AuthRemote,
    private val netManager: NetManager,
    private val preferences: SharedPreferences
) : BaseRepository() {

    fun register(email: String, password: String) = retrieveResourceAsFlow {
        if (netManager.isConnectedToInternet()) {
            remote.register(
                email = email,
                password = password,
            )
        } else throw NoInternetException()
    }

    fun login(email: String, password: String, token: String? = null) = retrieveResourceAsFlow {
        if (netManager.isConnectedToInternet()) remote.login(
            email = email,
            password = password,
            token = token
        )
        else throw NoInternetException()
    }

    fun reset(token: String, newPassword: String) = retrieveResourceAsFlow {
        if (netManager.isConnectedToInternet()) {
            remote.reset(
                token = token,
                newPassword = newPassword,
            )
        } else throw NoInternetException()
    }

    fun forgotPassword(email:String) = retrieveResourceAsFlow {
        if(netManager.isConnectedToInternet()){
            remote.forgotPassword(
                email=email
            )
        }else throw NoInternetException()
    }

    fun refreshToken(token:String) = runBlocking {
        if(netManager.isConnectedToInternet()){
           remote.refreshToken(token)
        }else throw NoInternetException()
    }
}