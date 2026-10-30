package jp.plesion.app.core.location

import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import jp.plesion.app.data.model.GeoPoint
import kotlin.math.round


data class GeohashRange(val start: String, val end: String)

// 位置計算
object GeoUtils {

  // 保存前に小数第３位に丸める
  fun round (point: GeoPoint): GeoPoint = GeoPoint(
    latitude = round(point.latitude * 1000) / 1000,
    longitude = round(point.longitude * 1000) / 1000,
  )

  // 2点間の距離
  fun distanceMeters(a: GeoPoint, b: GeoPoint): Double = 
    GeoFireUtils.getDistanceBetween(a.toGeoLocation(), b.toGeoLocation())

  // 投稿の保存時につける geohash
  fun geohash(point: GeoPoint): String = 
    GeoFireUtils.getGeoHashForLocation(point.toGeoLocation())

  // 中心から半径radiusMetersの円を覆うgeohashの範囲
  fun queryRanges(center: GeoPoint, radiusMeters: Double): List<GeohashRange> = 
    GeoFireUtils.getGeoHashQueryBounds(center.toGeoLocation(), radiusMeters)
      .map{ GeohashRange(start = it.startHash, end = it.endHash) }

  private fun GeoPoint.toGeoLocation() = GeoLocation(latitude, longitude)
}
