package com.dsd.kosjenka.domain.repository

import com.dsd.kosjenka.data.remote.BaseRepository
import com.dsd.kosjenka.domain.remote.ExerciseRemote
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val remote: ExerciseRemote,
) : BaseRepository() {
    fun getExercises(orderBy: String, order: String, category: String?, query: String?) =
        remote.getExercises(
            orderBy = orderBy,
            order = order,
            category = category,
            query = query,
        )
}