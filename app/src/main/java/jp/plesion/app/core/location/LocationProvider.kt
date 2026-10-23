package jp.plesion.app.core.location

import jp.plesion.app.data.model.GeoPoint

/**
 * 現在地を取得する窓口。
 * TODO: Fused Location Provider（play-services-location）で実装する。
 * 実装時は ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION の権限リクエストも必要。
 */
interface LocationProvider {
    suspend fun getCurrentLocation(): Result<GeoPoint>
}
