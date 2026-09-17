package com.neoship.courier.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.neoship.courier.BuildConfig
import com.neoship.courier.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Gère la mise à jour automatique de l'application.
 * Vérifie la version sur le serveur, télécharge l'APK et lance l'installation.
 */
class UpdateManager {

    private val api = RetrofitClient.apiService
    private val currentVersionCode = BuildConfig.VERSION_CODE

    /**
     * Vérifie si une mise à jour est disponible.
     * @return Pair(updateDispo: Boolean, apkUrl: String?)
     */
    suspend fun checkForUpdate(): Result<Pair<Boolean, String>> {
        return try {
            val response = api.getAppVersion()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.latestVersion > currentVersionCode) {
                    Result.success(Pair(true, body.apkUrl))
                } else {
                    Result.success(Pair(false, ""))
                }
            } else {
                Result.failure(Exception("Erreur serveur (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Serveur indisponible"))
        }
    }

    /**
     * Télécharge l'APK et retourne le chemin du fichier.
     */
    suspend fun downloadApk(context: Context, apkUrl: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "apk_updates")
            dir.mkdirs()
            val file = File(dir, "neoship-update.apk")
            if (file.exists()) file.delete()

            val connection = URL(apkUrl).openConnection().apply {
                connectTimeout = 30000
                readTimeout = 30000
            }
            connection.connect()

            val inputStream = connection.getInputStream()
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            outputStream.close()
            inputStream.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(Exception("Échec du téléchargement : ${e.localizedMessage}"))
        }
    }

    /**
     * Lance l'installation de l'APK téléchargée.
     */
    fun installApk(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
}