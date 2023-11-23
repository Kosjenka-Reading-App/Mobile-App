package com.dsd.kosjenka.presentation.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.kosjenka.domain.repository.AuthRepository
import com.dsd.kosjenka.utils.UiStates
import com.dsd.kosjenka.utils.error.EmailUnavailableException
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
class ResetPasswordView @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel(){

    private val _eventFlow = MutableSharedFlow<UiStates>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val handler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            _eventFlow.emit(
                when (exception) {
                    is EmailUnavailableException -> UiStates.EMAIL_UNAVAILABLE
                    is NoInternetException -> UiStates.NO_INTERNET_CONNECTION
                    else -> {
                        Timber.e("Exception: ${exception.localizedMessage}")
                        UiStates.UNKNOWN_ERROR
                    }
                }
            )
        }
    }

    /*fun reset(token: String, password: String) {
        viewModelScope.launch(handler) {
            authRepository.reset(
                token = token,
                newPassword = password
            ).collect {
                if (it != null) _eventFlow.emit(UiStates.RESET_PASSWORD)
                else _eventFlow.emit(UiStates.UNKNOWN_ERROR)
            }
        }
    }*/

    fun forgotPassword(email:String){
        viewModelScope.launch(handler) {
            authRepository.forgotPassword(
                email=email
            ).collect{
                if (it != null) _eventFlow.emit(UiStates.RESET_PASSWORD)
                else _eventFlow.emit(UiStates.UNKNOWN_ERROR)
            }
        }
    }


}