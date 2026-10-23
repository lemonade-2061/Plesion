package jp.plesion.app.data.model

/** Firebase の GeoPoint に依存しないための独自型。data 層の外に Firebase の型を漏らさない。 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)
