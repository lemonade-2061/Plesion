package jp.plesion.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import jp.plesion.app.data.model.User
import jp.plesion.app.data.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Firebase Authentication（Google）を使う実装。 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthRepository {
    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(credential).await().user
            ?: error("Firebase returned no user")
        user.toUser()
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}

// bio は Firestore の users に保存するので、ここでは扱わない
private fun FirebaseUser.toUser(): User = User(
    id = uid,
    displayName = displayName.orEmpty(),
    photoUrl = photoUrl?.toString(),
)
