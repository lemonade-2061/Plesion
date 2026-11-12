package jp.plesion.app.core.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * 選んだ画像をアップロード用の JPEG にする。
 * 写真の EXIF には撮影場所（GPS）が入っているので、必ず再エンコードして消す（DATA_CONTRACT §4）。
 */
interface ImageCompressor {
    suspend fun compress(uri: Uri): Result<ByteArray>
}

class AndroidImageCompressor(private val contentResolver: ContentResolver) : ImageCompressor {
    override suspend fun compress(uri: Uri): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decode(uri)
            ByteArrayOutputStream().use { out ->
                // Bitmap から書き出すので EXIF は引き継がれない
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) { "Failed to encode image" }
                bitmap.recycle()
                out.toByteArray()
            }
        }
    }

    // 長い辺が MAX_SIDE_PX に収まるように縮小して読み込む
    private fun decode(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder は EXIF の向きも反映してくれる
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
                val scale = MAX_SIDE_PX.toFloat() / max(info.size.width, info.size.height)
                if (scale < 1f) {
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            // Android 8.x（API 26〜27）は向きの補正をしない（対象端末が少ないため）
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sampleSize = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIDE_PX) sampleSize *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Failed to decode image")
        }

    private companion object {
        const val MAX_SIDE_PX = 1600
        const val JPEG_QUALITY = 80
    }
}
