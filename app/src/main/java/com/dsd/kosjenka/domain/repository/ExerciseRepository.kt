package com.dsd.kosjenka.domain.repository

import com.dsd.kosjenka.data.remote.BaseRepository
import com.dsd.kosjenka.domain.models.Completion
import com.dsd.kosjenka.domain.remote.ExerciseRemote
import com.dsd.kosjenka.utils.NetManager
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.error.NoInternetException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class  ExerciseRepository @Inject constructor(
    private val remote: ExerciseRemote,
    private val netManager: NetManager,
    private val preferences: SharedPreferences,
) : BaseRepository() {
    fun getExercises(orderBy: String, order: String, category: String?, query: String?) =
        remote.getExercises(
            orderBy = orderBy,
            order = order,
            category = category,
            query = query,
        )

    fun getCategories() = retrieveResourceAsFlow {
        preferences.categories = emptyList()
        if (netManager.isConnectedToInternet()) {
            val categories = remote.getCategories()
            if (categories != null)
                preferences.categories = categories.items
        } else
            throw NoInternetException()
    }

    fun getExercise(exerciseId: Int, userId: String) = retrieveResourceAsFlow {
        if (netManager.isConnectedToInternet())
            remote.getExercise(exerciseId, userId)
         else
            throw NoInternetException()
    }

    fun updateCompletion(exerciseId: Int, completion: Completion) = retrieveResourceAsFlow {
        if (netManager.isConnectedToInternet())
            remote.updateCompletion(exerciseId = exerciseId, completion = completion)
        else
            throw NoInternetException()
    }

}