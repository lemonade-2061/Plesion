package jp.plesion.app.data.model

data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
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
    }
}
