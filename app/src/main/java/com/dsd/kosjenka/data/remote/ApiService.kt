package com.dsd.kosjenka.data.remote

import com.dsd.kosjenka.domain.models.Exercise
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {

    @GET("exercises")
    suspend fun getExercises(): Response<ArrayList<Exercise>>

}