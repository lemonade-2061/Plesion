package jp.plesion.app.data.model

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val createdAt: Long,
)
