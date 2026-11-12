package jp.plesion.app

import android.net.Uri
import jp.plesion.app.core.location.LocationProvider
import jp.plesion.app.data.local.FakePostRepository
import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.CompletableDeferred

/** 東京駅付近（SampleData の投稿がある場所）。 */
val TOKYO_STATION = GeoPoint(35.681236, 139.767125)

class TestLocationProvider(var result: Result<GeoPoint> = Result.success(TOKYO_STATION)) : LocationProvider {
    var calls = 0
    /** null 以外なら、完了するまで現在地の取得を止める（取得中の状態を作るため）。 */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun getCurrentLocation(): Result<GeoPoint> {
        calls++
        // 実機と同じく「呼ばれた時点の位置」を返す（待機中に result を変えても反映しない）
        val snapshot = result
        gate?.await()
        return snapshot
    }
}

/** FakePostRepository に委譲しつつ、失敗を差し込めるようにしたもの。 */
class TestPostRepository(
    private val delegate: PostRepository = FakePostRepository(),
) : PostRepository by delegate {
    var nearbyFailure: Throwable? = null
    var postFailure: Throwable? = null
    var commentsFailure: Throwable? = null
    val nearbyCenters = mutableListOf<Pair<GeoPoint, Double>>()
    val createdLocations = mutableListOf<GeoPoint?>()
    val createdAnonymous = mutableListOf<Boolean>()
    /** null 以外なら、検索時点の結果を返す前に完了を待つ（通信中に投稿が増えた状況を作るため）。 */
    var nearbyGate: CompletableDeferred<Unit>? = null

    override suspend fun getNearbyPosts(center: GeoPoint, radiusMeters: Double): Result<List<Post>> {
        nearbyCenters += center to radiusMeters
        nearbyFailure?.let { return Result.failure(it) }
        val snapshot = delegate.getNearbyPosts(center, radiusMeters)
        nearbyGate?.await()
        return snapshot
    }

    override suspend fun getPost(postId: String): Result<Post> {
        postFailure?.let { return Result.failure(it) }
        return delegate.getPost(postId)
    }

    override suspend fun getComments(postId: String): Result<List<Comment>> {
        commentsFailure?.let { return Result.failure(it) }
        return delegate.getComments(postId)
    }

    override suspend fun createPost(
        text: String,
        location: GeoPoint?,
        isAnonymous: Boolean,
        imageUris: List<Uri>,
    ): Result<Post> {
        createdLocations += location
        createdAnonymous += isAnonymous
        return delegate.createPost(text, location, isAnonymous, imageUris)
    }
}
