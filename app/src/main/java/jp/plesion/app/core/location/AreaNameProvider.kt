package jp.plesion.app.core.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import jp.plesion.app.data.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/** 位置から、ホーム上部に出すエリア名（市区町村名など）を求める。分からなければ null。 */
fun interface AreaNameProvider {
    suspend fun getAreaName(point: GeoPoint): String?
}

/**
 * 端末の Geocoder で市区町村名を求める。
 * 送る位置は丸めてから使う（市区町村が分かれば十分で、正確な位置を外に出す必要はないため）。
 */
class GeocoderAreaNameProvider(private val context: Context) : AreaNameProvider {
    override suspend fun getAreaName(point: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        val rounded = GeoUtils.round(point)
        val geocoder = Geocoder(context, Locale.JAPAN)
        val address = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(
                        rounded.latitude,
                        rounded.longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses.firstOrNull())
                            override fun onError(errorMessage: String?) = cont.resume(null)
                        },
                    )
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(rounded.latitude, rounded.longitude, 1)?.firstOrNull()
                }
            }
        }.getOrNull()
        // 東京23区などは locality が空で subLocality に区名が入ることがある
        return address?.let { it.locality ?: it.subLocality ?: it.subAdminArea ?: it.adminArea }
    }
}
