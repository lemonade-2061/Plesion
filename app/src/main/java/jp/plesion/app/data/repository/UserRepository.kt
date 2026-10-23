package jp.plesion.app.data.repository

import jp.plesion.app.data.model.User

interface UserRepository {
    suspend fun getUser(userId: String): Result<User>

    suspend fun updateProfile(user: User): Result<Unit>
}
