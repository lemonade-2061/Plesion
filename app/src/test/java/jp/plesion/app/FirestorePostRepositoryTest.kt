package jp.plesion.app

import jp.plesion.app.data.model.Post
import jp.plesion.app.data.remote.validatePostText
import org.junit.Test

class FirestorePostRepositoryTest {
    @Test
    fun validatePostText_acceptsUpToMaxLength() {
        validatePostText("a")
        validatePostText("a".repeat(Post.MAX_TEXT_LENGTH))
    }

    @Test(expected = IllegalArgumentException::class)
    fun validatePostText_rejectsBlank() {
        validatePostText("  \n")
    }

    @Test(expected = IllegalArgumentException::class)
    fun validatePostText_rejectsTooLong() {
        validatePostText("a".repeat(Post.MAX_TEXT_LENGTH + 1))
    }
}
