package com.dsd.kosjenka.data.remote

import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.domain.response_objects.LoginResponseObject
import com.dsd.kosjenka.domain.models.User
import com.dsd.kosjenka.domain.response_objects.RegisterResponseObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("register")
    suspend fun register(
        @Body user: User,
    ): Response<RegisterResponseObject>

    @POST("login")
    suspend fun login(
        @Body user: User,
    ): Response<LoginResponseObject>

    @GET("exercises")
    suspend fun getExercises(
        @Query("skip") skip: Int,
        @Query("limit") limit: Int,
        @Query("order_by") orderBy: String,
        @Query("order") order: String,
        @Query("category") category: String?,
        @Query("title_like") query: String?,
    ): Response<ArrayList<Exercise>>

}