package jp.plesion.app

import jp.plesion.app.core.location.GeoUtils
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.remote.filterNearby
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {
    private val center = GeoPoint(35.681, 139.767)

    // 緯度 0.001 度 ≒ 111m。保存位置は 100m 単位に丸まるので境界から少し離して置く
    private val inside = GeoPoint(center.latitude + 0.008, center.longitude) // 約 890m
    private val outside = GeoPoint(center.latitude + 0.010, center.longitude) // 約 1110m

    @Test
    fun round_roundsToThreeDecimals() {
        assertEquals(GeoPoint(35.682, 139.767), GeoUtils.round(GeoPoint(35.68157, 139.76749)))
    }

    @Test
    fun distanceMeters_insideAndOutsideOneKm() {
        assertTrue(GeoUtils.distanceMeters(center, inside) <= 1_000.0)
        assertTrue(GeoUtils.distanceMeters(center, outside) > 1_000.0)
    }

    @Test
    fun queryRanges_coverPointWithinRadius() {
        val hash = GeoUtils.geohash(inside)
        assertTrue(GeoUtils.queryRanges(center, 1_000.0).any { hash >= it.start && hash <= it.end })
    }

    @Test
    fun filterNearby_keepsOnlyPostsWithinOneKm_newestFirst() {
        val posts = listOf(
            post("near-old", inside, createdAt = 1),
            post("far", outside, createdAt = 2),
            post("no-location", null, createdAt = 3),
            post("near-new", center, createdAt = 4),
        )

        val result = filterNearby(posts, center, 1_000.0)

        assertEquals(listOf("near-new", "near-old"), result.map { it.id })
    }

    private fun post(id: String, location: GeoPoint?, createdAt: Long) = Post(
        id = id,
        authorId = "test-user",
        authorName = "テスト",
        text = "test",
        location = location,
        createdAt = createdAt,
    )
}
