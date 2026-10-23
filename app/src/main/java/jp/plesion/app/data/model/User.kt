package jp.plesion.app.data.model

data class User(
    val id: String,
    val displayName: String,
    val photoUrl: String? = null,
    val bio: String = "",
)
