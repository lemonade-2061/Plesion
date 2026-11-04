package jp.plesion.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import jp.plesion.app.data.model.User
import jp.plesion.app.data.repository.UserRepository
import kotlinx.coroutines.tasks.await

/** Firestore の `users/{uid}` を使う実装。項目は docs/DATA_CONTRACT.md §2 に合わせる。 */
class FirestoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : UserRepository {
    private val users get() = firestore.collection("users")

    // 自分のドキュメントがまだなければ、Google アカウントの名前と画像で作る。
    // ログイン時ではなくここで作るのは、作成前からログイン済みのユーザーも取りこぼさないため
    override suspend fun getUser(userId: String): Result<User> = runCatching {
        val snapshot = users.document(userId).get().await()
        if (snapshot.exists()) return@runCatching snapshot.toUser() ?: error("User is malformed: $userId")
        val me = auth.currentUser
        if (me == null || me.uid != userId) throw NoSuchElementException("User not found: $userId")
        val user = User(
            id = me.uid,
            displayName = me.displayName.orEmpty().take(MAX_DISPLAY_NAME_LENGTH),
            photoUrl = me.photoUrl?.toString(),
        )
        users.document(userId).set(user.toMap()).await()
        user
    }

    override suspend fun updateProfile(user: User): Result<Unit> = runCatching {
        check(user.id == auth.currentUser?.uid) { "Cannot update another user's profile" }
        users.document(user.id).set(user.toMap()).await()
    }
}

/** セキュリティルールの上限と合わせる。 */
private const val MAX_DISPLAY_NAME_LENGTH = 50

// id はドキュメント ID なので項目には入れない
private fun User.toMap(): Map<String, Any?> = mapOf(
    "displayName" to displayName,
    "photoUrl" to photoUrl,
    "bio" to bio,
)

private fun DocumentSnapshot.toUser(): User? = User(
    id = id,
    displayName = getString("displayName") ?: return null,
    photoUrl = getString("photoUrl"),
    bio = getString("bio").orEmpty(),
)
