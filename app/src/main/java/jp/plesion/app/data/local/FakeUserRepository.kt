package jp.plesion.app.data.local

import jp.plesion.app.data.model.User
import jp.plesion.app.data.repository.UserRepository

/** 【仮実装】SampleData を返すだけ。更新は保存されない。 */
class FakeUserRepository : UserRepository {
    override suspend fun getUser(userId: String): Result<User> {
        val user = SampleData.users.find { it.id == userId }
            ?: return Result.failure(NoSuchElementException("User not found: $userId"))
        return Result.success(user)
    }

    override suspend fun updateProfile(user: User): Result<Unit> = Result.success(Unit)
}
