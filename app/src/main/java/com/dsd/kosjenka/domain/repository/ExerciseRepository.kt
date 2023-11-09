package com.dsd.kosjenka.domain.repository

import com.dsd.kosjenka.data.remote.BaseRepository
import com.dsd.kosjenka.domain.remote.ExerciseRemote
import com.dsd.kosjenka.utils.NetManager
import com.vosaa.kosjenka.utils.error.NoInternetException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val remote: ExerciseRemote,
    private val netManager: NetManager,
) : BaseRepository() {

    fun getExercises() = retrieveResourceAsFlow {
        if (netManager.isConnectedToInternet()) {
            remote.getExercises()
        } else throw NoInternetException()
    }

}