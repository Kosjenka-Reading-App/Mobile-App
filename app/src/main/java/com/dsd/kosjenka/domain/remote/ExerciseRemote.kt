package com.dsd.kosjenka.domain.remote

import com.dsd.kosjenka.data.remote.ApiService
import com.dsd.kosjenka.data.remote.BaseRemote
import com.vosaa.kosjenka.utils.error.ErrorManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRemote @Inject constructor(
    private val apiService: ApiService,
    errorManager: ErrorManager,
) : BaseRemote(errorManager) {

    suspend fun getExercises() = parseResult {
        apiService.getExercises()
    }

}