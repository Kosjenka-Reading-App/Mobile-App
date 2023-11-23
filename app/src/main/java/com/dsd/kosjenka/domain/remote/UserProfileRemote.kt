package com.dsd.kosjenka.domain.remote

import android.util.Log
import com.dsd.kosjenka.data.remote.ApiService
import com.dsd.kosjenka.data.remote.BaseRemote
import com.dsd.kosjenka.domain.models.UserProfile
import com.dsd.kosjenka.domain.request_objects.CreateUserRequestObject
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.error.ErrorManager
import javax.inject.Inject

class UserProfileRemote @Inject constructor(
    private val apiService: ApiService,
    errorManager: ErrorManager,
) : BaseRemote(errorManager) {

    private var currentOffset = 0
    suspend fun getUserProfiles(token: String) = parseResult {
//        Log.d("TOKEN", token)
        apiService.getUsers("Bearer $token", currentOffset, 20)
    }

    suspend fun createUserProfile(token: String, userProfile: UserProfile) = parseResult {
//        Log.d("TOKEN", token)
        apiService.addUser("Bearer $token", CreateUserRequestObject(userProfile.username, userProfile.proficiency.toInt()))
    }

    suspend fun editUserProfile(token: String, userProfile: UserProfile) = parseResult {
        apiService.editUser("Bearer $token", userProfile.id_user, CreateUserRequestObject(userProfile.username, userProfile.proficiency.toInt()))
    }
}