package com.example.tripglide.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * OpenDota API Service
 * Documentation: https://docs.opendota.com/
 */
interface OpenDotaService {
    
    /**
     * Get player profile by account_id (Steam Friend ID)
     * Returns player profile and rank information
     */
    @GET("players/{account_id}")
    suspend fun getPlayerProfile(
        @Path("account_id") accountId: String
    ): Response<DotaPlayerResponse>
    
    /**
     * Get player win/loss record
     */
    @GET("players/{account_id}/wl")
    suspend fun getPlayerWinLoss(
        @Path("account_id") accountId: String
    ): Response<DotaWinLossResponse>
    
    /**
     * Get player recent matches (limit 20)
     */
    @GET("players/{account_id}/recentMatches")
    suspend fun getRecentMatches(
        @Path("account_id") accountId: String
    ): Response<List<DotaRecentMatch>>
    
    companion object {
        private const val BASE_URL = "https://api.opendota.com/api/"
        
        fun create(): OpenDotaService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenDotaService::class.java)
        }
    }
}
