package jp.plesion.app.data.local

import jp.plesion.app.data.model.User
import jp.plesion.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 【仮実装】どんなトークンでも SampleData.me としてログインした扱いにする。 */
class FakeAuthRepository : AuthRepository {
    private val user = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = user

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        user.value = SampleData.me
        return Result.success(SampleData.me)
    }

    override suspend fun signOut() {
        user.value = null
    }
}
