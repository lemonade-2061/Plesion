package jp.plesion.app.navigation

/** 画面のルート名。新しい画面を追加するときはここに1行足す。 */
object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CREATE_POST = "create_post"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val POST_DETAIL_ARG = "postId"
    const val POST_DETAIL = "post/{$POST_DETAIL_ARG}"

    fun postDetail(postId: String) = "post/${android.net.Uri.encode(postId)}"
}
