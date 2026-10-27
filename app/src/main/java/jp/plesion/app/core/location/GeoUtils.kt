package jp.plesion.app.core.location

import jp.plesion.app.data.model.GeoPoint

data class GeohashRange(val start: String, var end: String)

// 位置計算
object GeoUtils {

  // 保存前に小数第３位に丸める
  fun round (point: GeoPoint): GeoPoint = TODO()

  // 2転換の距離
  fun distanceMeters(a: GeoPoint, b: GeoPoint): Double = TODO()

  // 投稿の保存時につける geohash
  fun geohash(point: GeoPoint): String = TODO()

  // 中心から半径radiusMetersの円を覆うgeohashの範囲
  fun queryRanges(center: GeoPoint, radiusMeters: Double): List<GeohashRange> = TODO()
}
