package com.dsd.kosjenka.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.kosjenka.domain.repository.AuthRepository
import com.dsd.kosjenka.domain.response_objects.LoginResponseObject
import com.dsd.kosjenka.utils.UiStates
import com.dsd.kosjenka.utils.UiStates.*
import com.dsd.kosjenka.utils.error.EmailUnavailableException
import com.dsd.kosjenka.utils.error.NoInternetException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _tokenDataFlow = MutableSharedFlow<LoginResponseObject>()
    val tokenDataFlow = _tokenDataFlow.asSharedFlow()

    private val _eventFlow = MutableSharedFlow<UiStates>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val handler = CoroutineExceptionHandler { _, exception ->
        viewModelScope.launch {
            _eventFlow.emit(
                when (exception) {
                    is EmailUnavailableException -> EMAIL_UNAVAILABLE
                    is NoInternetException -> NO_INTERNET_CONNECTION
                    else -> {
                        Timber.e("Exception: ${exception.localizedMessage}")
                        UNKNOWN_ERROR
                    }
                }
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch(handler) {
            authRepository.login(
                email = email,
                password = password
            ).collect {
                if (it != null) _tokenDataFlow.emit(it)
                _eventFlow.emit(SUCCESS)
            }
        }
    }

//    fun resetPassword(email: String) {
//        viewModelScope.launch(handler) {
//            authRepository.resetPassword(
//                email = email
//            ).collect {
//                _eventFlow.emit(RESET_PASSWORD)
//            }
//        }
//    }
}