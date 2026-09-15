package com.neoship.courier.data.api.models

data class GpsUpdateRequest(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val courierId: String
)