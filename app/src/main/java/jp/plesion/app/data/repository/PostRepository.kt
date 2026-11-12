package jp.plesion.app.data.repository

import android.net.Uri
import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post

interface PostRepository {
    /**
     * [center] から [radiusMeters] 以内の投稿。新しい順。
     * 全件を取るタイムラインは位置情報の公開範囲を超えるので用意しない（DATA_CONTRACT §3）。
     */
    suspend fun getNearbyPosts(center: GeoPoint, radiusMeters: Double = 1_000.0): Result<List<Post>>

    suspend fun getPost(postId: String): Result<Post>

    /** ログイン中のユーザーの投稿。新しい順。自分の匿名投稿も含む（本人にだけ返す）。 */
    suspend fun getMyPosts(): Result<List<Post>>

    /**
     * [isAnonymous] なら投稿者名を「匿名」にし、uid は本人しか読めない場所に保存する（DATA_CONTRACT §4）。
     * [imageUris] は Photo Picker で選んだ画像（[Post.MAX_IMAGES] 枚まで）。縮小・位置情報の削除・アップロードはここで行う。
     */
    suspend fun createPost(
        text: String,
        location: GeoPoint?,
        isAnonymous: Boolean = false,
        imageUris: List<Uri> = emptyList(),
    ): Result<Post>

    /** 本文だけ編集できる（画像・位置・匿名かどうかは変えられない）。投稿者以外は失敗する。編集後の投稿を返す。 */
    suspend fun updatePost(postId: String, text: String): Result<Post>

    suspend fun deletePost(postId: String): Result<Unit>

    /**
     * ログイン中のユーザーの投稿か。編集・削除ボタンを出すかの判定に使う。
     * 匿名投稿は authorId が空なので、画面側で authorId と比べずにこれを使う。
     */
    suspend fun isMyPost(post: Post): Result<Boolean>

    /** 古い順。 */
    suspend fun getComments(postId: String): Result<List<Comment>>

    /** 投稿者はログイン中のユーザー。投稿の commentCount も 1 増やす。 */
    suspend fun addComment(postId: String, text: String): Result<Comment>
}
