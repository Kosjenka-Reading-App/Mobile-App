package com.dsd.kosjenka.presentation.user_profiles

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dsd.kosjenka.domain.models.UserProfile
import com.dsd.kosjenka.domain.repository.UserProfileRepository
import com.dsd.kosjenka.domain.response_objects.LoginResponseObject
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.UiStates
import com.dsd.kosjenka.utils.error.NoInternetException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UserProfilesViewModel @Inject constructor(
    private val repository: UserProfileRepository,
) : ViewModel() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val _profileDataFlow = MutableSharedFlow<ArrayList<UserProfile>>()
    val profileDataFlow = _profileDataFlow.asSharedFlow()

    private val _eventFlow = MutableSharedFlow<UiStates>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val handler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            _eventFlow.emit(
                when (exception) {
                    is NoInternetException -> UiStates.NO_INTERNET_CONNECTION
                    else -> {
                        Timber.e("Exception: ${exception.localizedMessage}")
                        UiStates.UNKNOWN_ERROR
                    }
                }
            )
        }
    }

    fun getUsers() {
        viewModelScope.launch(handler) {
            repository.getUserProfiles(
                sharedPreferences.accessToken
            ).collect{

                if (it != null){
//                    Log.d("UPVM", it.toString())
                    _profileDataFlow.emit(it)
                }
            }
        }
    }

    fun addUser(usernaeme: String){
        viewModelScope.launch(handler) {
            repository.addUserProfile(
                sharedPreferences.accessToken,
                UserProfile(0,0,usernaeme, 1.0)
            ).collect {
                if (it != null) {
                    _eventFlow.emit(UiStates.SUCCESS)
                }
            }
        }
    }

    fun editUser(profile: UserProfile, usernaeme: String){
        viewModelScope.launch(handler) {
            profile.username = usernaeme
            repository.editUserProfile(
                sharedPreferences.accessToken,
                profile
            ).collect {
                if (it != null) {
                    _eventFlow.emit(UiStates.SUCCESS)
                }
            }
        }
    }
}