package com.dsd.kosjenka.domain.repository

import com.dsd.kosjenka.data.remote.BaseRepository
import com.dsd.kosjenka.domain.remote.AuthRemote
import com.dsd.kosjenka.utils.NetManager
import com.dsd.kosjenka.utils.error.NoInternetException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val remote: AuthRemote,
    private val netManager: NetManager,
) : BaseRepository() {

    fun register(email: String, password: String) = retrieveResourceAsFlow {
        if (netManager.isConnectedToInternet()) {
            remote.register(
                email = email,
                password = password,
            )
        } else throw NoInternetException()
    }

    fun login(email: String, password: String) = retrieveResourceAsFlow {
        if (netManager.isConnectedToInternet()) remote.login(
            email = email,
            password = password
        )
        else throw NoInternetException()
    }

}