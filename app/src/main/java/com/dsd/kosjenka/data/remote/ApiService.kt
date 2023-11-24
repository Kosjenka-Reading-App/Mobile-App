package com.dsd.kosjenka.data.remote

import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.domain.response_objects.LoginResponseObject
import com.dsd.kosjenka.domain.models.User
import com.dsd.kosjenka.domain.response_objects.RegisterResponseObject
import com.dsd.kosjenka.domain.models.UserProfile
import com.dsd.kosjenka.domain.request_objects.CreateUserRequestObject
import com.dsd.kosjenka.domain.request_objects.ForgotPasswordRequest
import com.dsd.kosjenka.domain.response_objects.DeleteUserResponseObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("register/")
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

    @GET("/users")
    suspend fun getUsers(
        @Header("Authorization") token: String,
        @Query("skip") skip: Int,
        @Query("limit") limit: Int
    ): Response<ArrayList<UserProfile>>

    @POST("/users")
    suspend fun addUser(
        @Header("Authorization") token: String,
        @Body createUserObj: CreateUserRequestObject
    ) : Response<UserProfile>

    @PATCH("/users/{user_id}")
    suspend fun editUser(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int,
        @Body createUserObj: CreateUserRequestObject
    ) : Response<UserProfile>

    @DELETE("/users/{user_id}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ) : Response<DeleteUserResponseObject>

    @POST("/password/forgot")
    suspend fun forgotPassword(
        @Body request:ForgotPasswordRequest
    ):Response<ForgotPasswordRequest>
}