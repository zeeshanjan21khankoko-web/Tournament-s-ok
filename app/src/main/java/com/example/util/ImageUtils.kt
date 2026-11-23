package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

object ImageUtils {
    suspend fun encodeUriToBase64Jpeg(
        context: Context,
        uri: Uri,
        maxDimension: Int = 720,
        maxBase64Length: Int = 180_000
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IllegalArgumentException("Unable to open selected image"))
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext Result.failure(IllegalArgumentException("Unsupported image format"))

            val largest = max(decoded.width, decoded.height)
            val scaled = if (largest > maxDimension) {
                val ratio = maxDimension.toFloat() / largest.toFloat()
                val newW = (decoded.width * ratio).roundToInt().coerceAtLeast(1)
                val newH = (decoded.height * ratio).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, newW, newH, true)
            } else {
                decoded
            }

            var quality = 78
            var encoded: String
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                encoded = "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                quality -= 12
            } while (encoded.length > maxBase64Length && quality >= 24)

            if (encoded.length > maxBase64Length) {
                return@withContext Result.failure(IllegalArgumentException("Image is too large even after compression. Please pick a smaller image."))
            }
            Result.success(encoded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decodeBase64ToImageBitmap(raw: String?): ImageBitmap? {
        if (raw.isNullOrBlank() || raw.startsWith("preset:")) return null
        return try {
            val payload = raw.substringAfter("base64,", raw)
            val bytes = Base64.decode(payload, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

object FormatUtils {
    private val pkrFormat = NumberFormat.getNumberInstance(Locale.US)
    private val dateTimeFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.US)

    fun formatPkr(amount: Long): String = "PKR ${pkrFormat.format(amount)}"

    fun formatTimestamp(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: Date()
        return dateTimeFormat.format(date)
    }
}
