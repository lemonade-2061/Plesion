package jp.plesion.app.core.location

import jp.plesion.app.data.model.GeoPoint

/**
 * 現在地を取得する窓口。
 * 正確な位置（ACCESS_FINE_LOCATION）が必須。権限リクエストでは Android 12 以降の仕様に合わせて
 * ACCESS_FINE_LOCATION と ACCESS_COARSE_LOCATION をまとめて要求する。
 */
interface LocationProvider {
    suspend fun getCurrentLocation(): Result<GeoPoint>
}
