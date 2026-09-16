package com.neoship.courier.data.api

import com.neoship.courier.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface API NeoShip — contrat V1.0.
 *
 * Les headers `Authorization: Bearer <token>` sont injectés automatiquement
 * par l'interceptor de RetrofitClient.tokenProvider.
 * Les méthodes n'ont PAS de @Header("Authorization") pour éviter la duplication.
 */
interface ApiService {

    // ── Authentification (pas de token nécessaire) ──
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ── Courses ──
    @GET("api/deliveries")
    suspend fun getDeliveries(): Response<DeliveriesResponse>

    @GET("api/deliveries/{id}")
    suspend fun getDeliveryDetail(
        @Path("id") id: String
    ): Response<DeliveryDetailResponse>

    // ── GPS ──
    @POST("api/gps/update")
    suspend fun updateGps(
        @Body request: GpsUpdateRequest
    ): Response<GpsResponse>

    // ── Validation OTP ──
    @POST("api/deliveries/validate")
    suspend fun validateDelivery(
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