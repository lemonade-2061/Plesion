package jp.plesion.app.data.local

import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.model.User

/**
 * 【仮データ】画面確認専用のダミー。実在のユーザー・投稿ではない。
 * 本番データと混同しないよう、ID は "sample-"、本文は "[サンプル]" で始める。
 */
object SampleData {
    const val ID_PREFIX = "sample-"
    const val TEXT_PREFIX = "[サンプル]"

    val me = User(
        id = "${ID_PREFIX}user-1",
        displayName = "サンプル太郎",
        bio = "$TEXT_PREFIX これは仮のプロフィールです",
    )

    private val other = User(
        id = "${ID_PREFIX}user-2",
        displayName = "サンプル花子",
    )

    val users = listOf(me, other)

    // 東京駅付近の座標（位置検索の動作確認用の仮の値）
    private val tokyoStation = GeoPoint(35.681236, 139.767125)

    val posts = listOf(
        Post(
            id = "${ID_PREFIX}post-1",
            authorId = me.id,
            authorName = me.displayName,
            text = "$TEXT_PREFIX 近くのカフェが落ち着いていて良かった",
            location = tokyoStation,
            createdAt = 1_790_000_000_000,
            commentCount = 1,
        ),
        Post(
            id = "${ID_PREFIX}post-2",
            authorId = other.id,
            authorName = other.displayName,
            text = "$TEXT_PREFIX 今日は駅前でイベントをやっています",
            location = GeoPoint(35.6800, 139.7690),
            createdAt = 1_789_990_000_000,
        ),
    )

    val comments = listOf(
        Comment(
            id = "${ID_PREFIX}comment-1",
            postId = "${ID_PREFIX}post-1",
            authorId = other.id,
            authorName = other.displayName,
            text = "$TEXT_PREFIX 行ってみたいです",
            createdAt = 1_790_000_100_000,
        ),
    )
}
