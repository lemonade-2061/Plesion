package jp.plesion.app.data.repository

import jp.plesion.app.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** ログイン中のユーザー。未ログインなら null。 */
    val currentUser: Flow<User?>

    /** Google の ID トークンで Firebase Authentication にログインする。 */
    suspend fun signInWithGoogle(idToken: String): Result<User>

    suspend fun signOut()
}
