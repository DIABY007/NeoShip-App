package com.neoship.courier.data.api

import com.neoship.courier.data.api.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/gps/update")
    suspend fun updateGps(@Body request: GpsUpdateRequest): Response<ApiResponse<Unit>>

    @POST("api/deliveries/validate")
    suspend fun validateDelivery(@Body request: DeliveryValidateRequest): Response<ApiResponse<Unit>>
}