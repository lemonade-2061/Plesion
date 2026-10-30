package jp.plesion.app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import jp.plesion.app.data.model.GeoPoint
import kotlinx.coroutines.tasks.await

/**
 * 正確な位置（FINE）の権限がないときに返す失敗。おおよその位置（COARSE）だけの場合も含む。
 * 画面側はこれを見て閲覧・投稿を制限し、権限設定の案内を表示する。
 */
class LocationPermissionDeniedException : SecurityException("Location permission is not granted")

/** 端末が現在地を返せなかったとき（位置情報 OFF など）の失敗。 */
class LocationUnavailableException : IllegalStateException("Current location is unavailable")

/**
 * Fused Location Provider で現在地を取得する。
 * 権限のリクエストはしない（各画面の担当）。FINE の権限がなければ例外を投げずに Result.failure を返す。
 * 返す位置は丸めない。丸めは投稿の保存時に行う（docs/DATA_CONTRACT.md §4）。
 */
class FusedLocationProvider(
    private val hasPermission: () -> Boolean,
    private val client: () -> FusedLocationProviderClient,
) : LocationProvider {

    constructor(context: Context) : this(
        hasPermission = { context.hasLocationPermission() },
        client = { LocationServices.getFusedLocationProviderClient(context) },
    )

    // 権限は hasPermission で確認済み。確認後に取り消された場合の SecurityException も runCatching で失敗にする
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<GeoPoint> {
        if (!hasPermission()) return Result.failure(LocationPermissionDeniedException())
        return runCatching {
            // BALANCED だと GPS しか使えない環境（エミュレータ・Wi-Fi のない屋外）で null になる。1回きりの取得なので電池への影響は小さい
            val cancellation = CancellationTokenSource()
            val location = client()
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                .await(cancellation)
                ?: throw LocationUnavailableException()
            GeoPoint(location.latitude, location.longitude)
        }
    }
}

// おおよその位置（COARSE）は誤差が 1km 前後あり、1km 検索の精度を保証できないため FINE を必須にする
private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
