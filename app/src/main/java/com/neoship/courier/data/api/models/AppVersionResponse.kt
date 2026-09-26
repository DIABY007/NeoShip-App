package com.neoship.courier.data.api.models

/**
 * Réponse de l'API pour GET /api/app/version.
 */
data class AppVersionResponse(
    val latestVersion: Int,
    val apkUrl: String,
    val beneficiaryNumber: String? = null,
    val forceUpdate: Boolean = false
)