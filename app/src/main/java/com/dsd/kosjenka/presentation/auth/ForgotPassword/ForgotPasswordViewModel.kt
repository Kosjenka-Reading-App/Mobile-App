package com.dsd.kosjenka.presentation.auth.ForgotPassword

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.kosjenka.R
import com.dsd.kosjenka.domain.repository.AuthRepository
import com.dsd.kosjenka.domain.response_objects.LoginResponseObject
import com.dsd.kosjenka.utils.Common.Companion.isValidEmail
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.UiStates
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
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    @Inject
    lateinit var preferences: SharedPreferences

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



