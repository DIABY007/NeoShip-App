package com.neoship.courier.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Génère un QR Code bitmap à partir d'un texte.
 * Utilisé pour encoder le code USSD Mobile Money.
 */
object QrCodeGenerator {

    fun generate(content: String, size: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Construit l'URI USSD pour le paiement Mobile Money.
     * Format : *144*2*1*BENEFICIAIRE*MONTANT#
     * Encodé en tel: pour que le scan ouvre le dialer.
     */
    fun buildUssdUri(beneficiaryNumber: String, amount: Int): String {
        // Le # doit être encodé en %23 pour éviter d'être interprété
        val ussd = "*144*2*1*${beneficiaryNumber}*${amount}%23"
        return "tel:$ussd"
    }
}