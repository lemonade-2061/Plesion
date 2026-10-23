package jp.plesion.app.di

import jp.plesion.app.data.local.FakeAuthRepository
import jp.plesion.app.data.local.FakePostRepository
import jp.plesion.app.data.local.FakeUserRepository
import jp.plesion.app.data.repository.AuthRepository
import jp.plesion.app.data.repository.PostRepository
import jp.plesion.app.data.repository.UserRepository

/**
 * 手動 DI のコンテナ。画面は具体クラスではなく Repository インターフェースだけを知る。
 *
 * Firebase 実装ができたら、ここの Fake* を Firestore* 等に差し替えるだけで
 * 画面側のコードを変えずに本番データへ切り替えられる。
 */
class AppContainer {
    val authRepository: AuthRepository = FakeAuthRepository()
    val userRepository: UserRepository = FakeUserRepository()
    val postRepository: PostRepository = FakePostRepository()
}
