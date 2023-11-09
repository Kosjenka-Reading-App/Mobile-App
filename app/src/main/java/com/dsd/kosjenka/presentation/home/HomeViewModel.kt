package com.dsd.kosjenka.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.domain.repository.ExerciseRepository
import com.dsd.kosjenka.utils.UiStates
import com.dsd.kosjenka.utils.UiStates.EMAIL_UNAVAILABLE
import com.dsd.kosjenka.utils.UiStates.NO_INTERNET_CONNECTION
import com.dsd.kosjenka.utils.UiStates.UNKNOWN_ERROR
import com.vosaa.kosjenka.utils.error.EmailUnavailableException
import com.vosaa.kosjenka.utils.error.NoInternetException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ExerciseRepository,
) : ViewModel() {

    private val _mainDataFlow = MutableSharedFlow<ArrayList<Exercise>>()
    val mainDataFlow = _mainDataFlow.asSharedFlow()

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

    fun getExercises() {
        viewModelScope.launch(handler) {
            _eventFlow.emit(UiStates.LOADING)
            repository.getExercises().collect {
                if (it != null) _mainDataFlow.emit(it)
                _eventFlow.emit(UiStates.SUCCESS)
            }
        }
    }

}