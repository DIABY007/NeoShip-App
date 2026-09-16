package com.neoship.courier.data.api

import com.neoship.courier.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Authentification ──
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ── Courses ──
    @GET("api/deliveries")
    suspend fun getDeliveries(
        @Header("Authorization") token: String
    ): Response<DeliveriesResponse>

    @GET("api/deliveries/{id}")
    suspend fun getDeliveryDetail(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<DeliveryDetailResponse>

    // ── GPS ──
    @POST("api/gps/update")
    suspend fun updateGps(
        @Header("Authorization") token: String,
        @Body request: GpsUpdateRequest
    ): Response<GpsResponse>

    // ── Validation OTP ──
    @POST("api/deliveries/validate")
    suspend fun validateDelivery(
        @Header("Authorization") token: String,
        @Body request: DeliveryValidateRequest
    ): Response<DeliveryValidateResponse>
}

data class GpsResponse(
    val success: Boolean
)

data class DeliveryDetailResponse(
    val delivery: DeliveryDto,
    val gpsLogs: List<GpsLogDto>? = null
)

data class GpsLogDto(
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)