package com.neoship.courier.data.local

import android.content.Context

/**
 * Stockage persistant des IDs de courses validées.
 * Utilise SharedPreferences simple (données non sensibles).
 * Survit au process death : au redémarrage, les courses déjà livrées restent filtrées.
 *
 * La synchronisation avec l'API (GET /deliveries → statut "livrée") sera faite en V2.
 * Pour la V1, on persiste localement pour éviter le chaos opérationnel.
 */
class CompletedDeliveriesStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Retourne l'ensemble des IDs de courses déjà validées.
     */
    fun getCompletedIds(): Set<String> {
        return prefs.getStringSet(KEY_COMPLETED, emptySet()) ?: emptySet()
    }

    /**
     * Ajoute un ID aux courses validées.
     */
    fun addCompletedId(id: String) {
        val current = getCompletedIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet(KEY_COMPLETED, current).apply()
    }

    /**
     * Vide le stockage (utilisé à la déconnexion).
     */
    fun clear() {
        prefs.edit().remove(KEY_COMPLETED).apply()
    }

    companion object {
        private const val PREFS_NAME = "neoship_completed_deliveries"
        private const val KEY_COMPLETED = "completed_ids"
    }
}