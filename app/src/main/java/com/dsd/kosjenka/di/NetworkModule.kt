package com.dsd.kosjenka.di

import com.dsd.kosjenka.BuildConfig
import com.dsd.kosjenka.data.remote.ApiService
import com.dsd.kosjenka.data.remote.AuthInterceptor
import com.dsd.kosjenka.data.remote.AuthAuthenticator
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.TokenManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideTokenManager(sharedPreferences: SharedPreferences): TokenManager = TokenManager(sharedPreferences)

    @Singleton
    @Provides
    fun provideOkHttpClient(
        authorizationInterceptor: AuthInterceptor,
        authAuthenticator: AuthAuthenticator
    ): OkHttpClient {
        val dispatcher = Dispatcher()
        dispatcher.maxRequests = 1
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
            .dispatcher(dispatcher)
            .addInterceptor(authorizationInterceptor)
            .authenticator(authAuthenticator)
        if (BuildConfig.DEBUG)
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        return builder.build()
    }

    @Singleton
    @Provides
    fun provideApiService(client: OkHttpClient, gson: Gson): ApiService {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson)).client(client).build()
            .create(ApiService::class.java)
    }

    @Singleton
    @Provides
    fun provideGson(): Gson = Gson()
}