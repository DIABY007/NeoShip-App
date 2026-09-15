package com.neoship.courier.data.api.models

data class DeliveryValidateRequest(
    val deliveryId: String,
    val otp: String
)