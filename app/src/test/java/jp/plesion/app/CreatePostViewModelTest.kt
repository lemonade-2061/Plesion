package jp.plesion.app

import jp.plesion.app.data.local.FakePostRepository
import jp.plesion.app.feature.post.CreatePostViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// viewModelScope は Dispatchers.Main を使うため、テストでは差し替える
@OptIn(ExperimentalCoroutinesApi::class)
class CreatePostViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun submit_withText_marksPosted() {
        val viewModel = CreatePostViewModel(FakePostRepository())
        viewModel.onTextChange("テスト投稿")
        viewModel.submit()
        assertTrue(viewModel.uiState.value.isPosted)
    }

    @Test
    fun submit_withBlankText_showsError() {
        val viewModel = CreatePostViewModel(FakePostRepository())
        viewModel.submit()
        assertFalse(viewModel.uiState.value.isPosted)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }
}
