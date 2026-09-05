package com.iumrah.beta.core.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Bounded JPEG encoder for passport/receipt/chat uploads. It never persists a local copy.
 * The source Uri is decoded at a sampled size and compressed in memory only.
 */
object AndroidImageCodec {
    fun jpeg(resolver: ContentResolver, uri: Uri, maxDimension: Int = 2048, quality: Int = 86): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        var longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / sample > maxDimension * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("IMAGE_DECODE_FAILED")
        val scaled = if (max(decoded.width, decoded.height) > maxDimension) {
            val ratio = maxDimension.toFloat() / max(decoded.width, decoded.height).toFloat()
            Bitmap.createScaledBitmap(decoded, (decoded.width * ratio).toInt(), (decoded.height * ratio).toInt(), true).also {
                if (it !== decoded) decoded.recycle()
            }
        } else decoded
        return ByteArrayOutputStream().use { output ->
            check(scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(60, 95), output))
            scaled.recycle()
            output.toByteArray()
        }
    }
}
