package jp.plesion.app

import jp.plesion.app.core.ui.UiState
import jp.plesion.app.data.local.SampleData
import jp.plesion.app.feature.feed.detail.CommentInputState
import jp.plesion.app.feature.feed.detail.PostDetail
import jp.plesion.app.feature.feed.detail.PostDetailViewModel
import jp.plesion.app.feature.feed.detail.formatDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val post = SampleData.posts.first()

    @Test
    fun load_showsPostAndCommentsOfGivenId() {
        val viewModel = PostDetailViewModel(post.id, TestPostRepository())
        val detail = (viewModel.uiState.value as UiState.Success<PostDetail>).data
        assertEquals(post, detail.post)
        assertEquals(SampleData.comments.filter { it.postId == post.id }, detail.comments)
        assertEquals(1, detail.post.commentCount)
    }

    @Test
    fun sendComment_appendsCommentAndClearsInput() {
        val viewModel = PostDetailViewModel(post.id, TestPostRepository())
        val before = (viewModel.uiState.value as UiState.Success<PostDetail>).data

        viewModel.onCommentTextChange("いいね")
        viewModel.sendComment()

        val after = (viewModel.uiState.value as UiState.Success<PostDetail>).data
        assertEquals(before.comments.size + 1, after.comments.size)
        assertTrue(after.comments.last().text.endsWith("いいね"))
        assertEquals(before.post.commentCount + 1, after.post.commentCount)
        assertEquals(CommentInputState(), viewModel.commentInput.value)
    }

    @Test
    fun sendComment_blank_doesNothing() {
        val viewModel = PostDetailViewModel(post.id, TestPostRepository())
        val before = viewModel.uiState.value

        viewModel.onCommentTextChange("   ")
        viewModel.sendComment()

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun sendComment_failure_keepsTextAndShowsError() {
        val repository = TestPostRepository()
        val viewModel = PostDetailViewModel(post.id, repository)
        repository.addCommentFailure = RuntimeException("offline")

        viewModel.onCommentTextChange("いいね")
        viewModel.sendComment()

        assertEquals("いいね", viewModel.commentInput.value.text)
        assertFalse(viewModel.commentInput.value.isSending)
        assertNotNull(viewModel.commentInput.value.errorMessage)
    }

    @Test
    fun load_unknownId_showsNotFound() {
        val viewModel = PostDetailViewModel("missing", TestPostRepository())
        assertTrue((viewModel.uiState.value as UiState.Error).message.contains("見つかりません"))
    }

    @Test
    fun load_error_thenRetrySucceeds() {
        val repository = TestPostRepository().apply { postFailure = RuntimeException("offline") }
        val viewModel = PostDetailViewModel(post.id, repository)
        assertTrue(viewModel.uiState.value is UiState.Error)

        repository.postFailure = null
        viewModel.load()
        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun load_commentsError_showsError() {
        val repository = TestPostRepository().apply { commentsFailure = RuntimeException("offline") }
        val viewModel = PostDetailViewModel(post.id, repository)
        assertTrue(viewModel.uiState.value is UiState.Error)
    }

    @Test
    fun formatDateTime_usesGivenZone() {
        assertEquals("1970/01/01 09:00", formatDateTime(0, ZoneId.of("Asia/Tokyo")))
    }
}
