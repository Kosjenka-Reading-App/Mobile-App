package com.dsd.kosjenka.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.kosjenka.domain.repository.AuthRepository
import com.dsd.kosjenka.utils.UiStates
import com.dsd.kosjenka.utils.UiStates.EMAIL_UNAVAILABLE
import com.dsd.kosjenka.utils.UiStates.NO_INTERNET_CONNECTION
import com.dsd.kosjenka.utils.UiStates.REGISTER
import com.dsd.kosjenka.utils.UiStates.UNKNOWN_ERROR
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
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

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

    fun register(email: String, password: String) {
        viewModelScope.launch(handler) {
            authRepository.register(
                email = email, password = password
            ).collect {
                if (it != null) _eventFlow.emit(REGISTER)
                else _eventFlow.emit(UNKNOWN_ERROR)
            }
        }
    }

}