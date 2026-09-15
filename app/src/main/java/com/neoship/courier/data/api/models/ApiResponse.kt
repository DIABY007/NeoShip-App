package com.neoship.courier.data.api.models

/**
 * Wrapper générique pour uniformiser les réponses API.
 * Le backend Next.js renverra une structure { success, message, data? }.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)