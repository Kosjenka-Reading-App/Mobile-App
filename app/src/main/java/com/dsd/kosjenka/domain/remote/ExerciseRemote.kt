package com.dsd.kosjenka.domain.remote

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.liveData
import com.dsd.kosjenka.data.remote.ApiService
import com.dsd.kosjenka.data.remote.BaseRemote
import com.dsd.kosjenka.presentation.home.ExercisePagingSource
import com.vosaa.kosjenka.utils.error.ErrorManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRemote @Inject constructor(
    private val apiService: ApiService,
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
                    orderBy = orderBy,
                    order = order,
                    category = category,
                    query = query,
                )
            }
        ).liveData


}