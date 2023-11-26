package com.dsd.kosjenka.presentation.home.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.domain.repository.ExerciseRepository
import com.dsd.kosjenka.utils.UiStates
import com.dsd.kosjenka.utils.error.NoInternetException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository,
) : ViewModel() {

    private val _exerciseDataFlow = MutableSharedFlow<Exercise>()
    val exerciseDataFlow = _exerciseDataFlow.asSharedFlow()

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

    fun getExercise(exerciseId: Int) {
        viewModelScope.launch(handler) {
            _eventFlow.emit(UiStates.LOADING)
            repository.getExercise(
                exerciseId = exerciseId
            ).collect {
                if (it != null) _exerciseDataFlow.emit(it)
                _eventFlow.emit(UiStates.SUCCESS)
            }
        }
    }

}