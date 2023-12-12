package com.dsd.kosjenka.data.remote

import com.dsd.kosjenka.domain.models.Category
import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.domain.response_objects.LoginResponseObject
import com.dsd.kosjenka.domain.models.User
import com.dsd.kosjenka.domain.response_objects.RegisterResponseObject
import com.dsd.kosjenka.domain.models.UserProfile
import com.dsd.kosjenka.domain.request_objects.CreateUserRequestObject
import com.dsd.kosjenka.domain.request_objects.ForgotPasswordRequest
import com.dsd.kosjenka.domain.request_objects.ResetPasswordRequest
import com.dsd.kosjenka.domain.request_objects.ResetPasswordRequest2
import com.dsd.kosjenka.domain.response_objects.ForgotPasswordResponse
import com.dsd.kosjenka.domain.response_objects.ResetResponseObject
import com.dsd.kosjenka.domain.response_objects.DeleteUserResponseObject
import com.dsd.kosjenka.domain.response_objects.GetCategoriesResponseObject
import com.dsd.kosjenka.domain.response_objects.GetExercisesResponseObject
import com.dsd.kosjenka.domain.response_objects.GetUsersResponseObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.PATCH
import retrofit2.http.DELETE

interface ApiService {

    @POST("register")
    suspend fun register(
        @Body user: User,
    ): Response<RegisterResponseObject>

    @POST("/password/reset")
    suspend fun resetPassword(
        @Body request: ForgotPasswordRequest
    ): Response<ResetResponseObject>

    @POST("/password/forgot")
    suspend fun passwordForgot(
        @Body request: ForgotPasswordRequest
    ):Response<ForgotPasswordResponse>

    @POST("login")
    suspend fun login(
        @Body user: User,
    ): Response<LoginResponseObject>

    @GET("exercises")
    suspend fun getExercises(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("user_id") userId: String?,
        @Query("order_by") orderBy: String,
        @Query("order") order: String,
        @Query("category") category: String?,
        @Query("title_like") query: String?,
    ): Response<GetExercisesResponseObject>

    @GET("categories")
    suspend fun getCategories()
            : Response<GetCategoriesResponseObject>

    @GET("exercises/{exercise_id}")
    suspend fun getExercise(
        @Path("exercise_id") exerciseId: Int
    ):Response<Exercise>

    @GET("/users")
    suspend fun getUsers(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<GetUsersResponseObject>

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
}