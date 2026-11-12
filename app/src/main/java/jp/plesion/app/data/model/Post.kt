package jp.plesion.app.data.model

data class Post(
    val id: String,
    /** 投稿者の uid。匿名投稿では空文字（DATA_CONTRACT §4）。 */
    val authorId: String,
    val authorName: String,
    val isAnonymous: Boolean = false,
    val text: String,
    val imageUrls: List<String> = emptyList(),
    val location: GeoPoint? = null,
    /** エポックミリ秒。Firestore の Timestamp への変換は remote 層で行う。 */
    val createdAt: Long,
    val commentCount: Int = 0,
) {
    companion object {
        /** 本文の最大文字数（DATA_CONTRACT §2）。 */
        const val MAX_TEXT_LENGTH = 500

        /** 匿名投稿の表示名（DATA_CONTRACT §4）。 */
        const val ANONYMOUS_NAME = "匿名"
    }
}
