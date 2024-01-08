package com.dsd.kosjenka.domain.remote

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.liveData
import com.dsd.kosjenka.data.remote.ApiService
import com.dsd.kosjenka.data.remote.BaseRemote
import com.dsd.kosjenka.domain.models.Completion
import com.dsd.kosjenka.presentation.home.ExercisePagingSource
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.error.ErrorManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRemote @Inject constructor(
    private val apiService: ApiService,
    private val preferences: SharedPreferences,
    errorManager: ErrorManager,
) : BaseRemote(errorManager) {

    fun getExercises(orderBy: String, order: String, category: String?, query: String?) =
        Pager(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = false,
            ), pagingSourceFactory = {
                ExercisePagingSource(
                    apiService,
                    userId = if (orderBy == "complexity") null else preferences.userId,
                    orderBy = orderBy,
                    order = order,
                    category = category,
                    query = query,
                )
            }
        ).liveData


    suspend fun getCategories() =
        parseResult {
            apiService.getCategories()
        }

    suspend fun getExercise(exerciseId: Int, userId: String) = parseResult {
        apiService.getExercise(exerciseId, userId)
    }

    suspend fun updateCompletion(exerciseId: Int, completion: Completion) = parseResult {
        apiService.updateCompletion(exerciseId = exerciseId, completion = completion)
    }


}