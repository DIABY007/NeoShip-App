package com.neoship.courier.data.local

import org.json.JSONObject
import java.util.Base64

/**
 * Décodeur JWT minimaliste.
 * Extrait le payload (partie centrale) sans vérifier la signature.
 * Utilisé uniquement pour récupérer l'identifiant du coursier depuis le token stocké.
 *
 * Aucune librairie externe — utilise Base64.getUrlDecoder() (Java 8+) + org.json (Android SDK).
 */
object JwtDecoder {

    /**
     * Extrait l'ID du coursier depuis le payload JWT.
     * Cherche dans l'ordre : sub, courierId, courier_id, id, userId, user_id.
     * @return l'ID trouvé, ou null si impossible à décoder
     */
    fun getCourierId(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            val payload = parts[1]

            // Le padding base64url peut être tronqué — on le restitue
            val padded = payload.padEnd(
                payload.length + (4 - payload.length % 4) % 4, '='
            )

            val jsonBytes = Base64.getUrlDecoder().decode(padded)
            val json = String(jsonBytes, Charsets.UTF_8)
            val jsonObj = JSONObject(json)

            // Champs JWT typiques contenant l'identifiant
            val fieldCandidates = arrayOf(
                "sub", "courierId", "courier_id",
                "id", "userId", "user_id"
            )

            fieldCandidates
                .firstOrNull { jsonObj.has(it) }
                ?.let { jsonObj.optString(it, null) }
                ?.takeIf { it.isNotEmpty() }

        } catch (e: Exception) {
            android.util.Log.w("JwtDecoder", "Échec décodage JWT", e)
            null
        }
    }
}