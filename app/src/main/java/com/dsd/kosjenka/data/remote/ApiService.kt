package com.dsd.kosjenka.data.remote

import com.dsd.kosjenka.domain.models.Category
import com.dsd.kosjenka.domain.models.Exercise
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("exercises")
    suspend fun getExercises(
        @Query("skip") skip: Int,
        @Query("limit") limit: Int,
        @Query("order_by") orderBy: String,
        @Query("order") order: String,
        @Query("category") category: String?,
        @Query("title_like") query: String?
    ): Response<ArrayList<Exercise>>

}