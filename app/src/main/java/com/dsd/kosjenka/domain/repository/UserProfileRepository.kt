package com.dsd.kosjenka.domain.repository

import androidx.lifecycle.viewModelScope
import com.dsd.kosjenka.data.remote.BaseRepository
import com.dsd.kosjenka.domain.models.UserProfile
import com.dsd.kosjenka.domain.remote.UserProfileRemote
import com.dsd.kosjenka.presentation.auth.login.LoginViewModel
import com.dsd.kosjenka.utils.NetManager
import com.dsd.kosjenka.utils.error.NoInternetException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class UserProfileRepository @Inject constructor(
    private val remote: UserProfileRemote,
    private val netManager: NetManager
) : BaseRepository() {

    fun getUserProfiles(token: String) = retrieveResourceAsFlow {
        if (netManager.isConnectedToInternet()) remote.getUserProfiles(token)
        else throw NoInternetException()
    }

    fun addUserProfile(token: String, userProfile: UserProfile) = retrieveResourceAsFlow {
        remote.createUserProfile(token, userProfile)
    }

     fun editUserProfile(token: String, userProfile: UserProfile) = retrieveResourceAsFlow {
        remote.editUserProfile(token, userProfile)
    }

    fun deleteUserProfile(token: String, userProfile: UserProfile) = retrieveResourceAsFlow {
        remote.deleteUserProfile(token, userProfile)
    }

}