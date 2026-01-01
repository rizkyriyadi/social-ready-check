package com.example.tripglide.data.network

import com.example.tripglide.data.model.AppUpdateInfo
import retrofit2.http.GET

/**
 * Service for fetching OTA update information from GitHub raw content
 */
interface UpdateService {
    
    @GET("rizkyriyadi/social-ready-check/refs/heads/main/version.json")
    suspend fun checkForUpdate(): AppUpdateInfo
}
