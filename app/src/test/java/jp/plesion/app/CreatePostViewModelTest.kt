package jp.plesion.app

import jp.plesion.app.core.location.LocationPermissionDeniedException
import jp.plesion.app.core.location.LocationUnavailableException
import jp.plesion.app.feature.post.CreatePostViewModel
import kotlinx.coroutines.CompletableDeferred
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
        val viewModel = CreatePostViewModel(TestPostRepository(), TestLocationProvider())
        viewModel.onTextChange("テスト投稿")
        viewModel.submit()
        assertTrue(viewModel.uiState.value.isPosted)
    }

    @Test
    fun submit_withBlankText_showsError() {
        val viewModel = CreatePostViewModel(TestPostRepository(), TestLocationProvider())
        viewModel.submit()
        assertFalse(viewModel.uiState.value.isPosted)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun submit_passesCurrentLocationToRepository() {
        val repository = TestPostRepository()
        val viewModel = CreatePostViewModel(repository, TestLocationProvider())
        viewModel.onTextChange("テスト投稿")
        viewModel.submit()
        assertEquals(listOf(TOKYO_STATION), repository.createdLocations)
    }

    @Test
    fun submit_withoutPermission_doesNotPost() {
        val repository = TestPostRepository()
        val viewModel = CreatePostViewModel(repository, TestLocationProvider(Result.failure(LocationPermissionDeniedException())))
        viewModel.onTextChange("テスト投稿")
        viewModel.submit()
        assertFalse(viewModel.uiState.value.isPosted)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(repository.createdLocations.isEmpty())
    }

    @Test
    fun submit_locationUnavailable_showsError_thenRetrySucceeds() {
        val location = TestLocationProvider(Result.failure(LocationUnavailableException()))
        val viewModel = CreatePostViewModel(TestPostRepository(), location)
        viewModel.onTextChange("テスト投稿")
        viewModel.submit()
        assertNotNull(viewModel.uiState.value.errorMessage)

        location.result = Result.success(TOKYO_STATION)
        viewModel.submit()
        assertTrue(viewModel.uiState.value.isPosted)
    }

    @Test
    fun submit_whileLocating_ignoresSecondTap() {
        val repository = TestPostRepository()
        val location = TestLocationProvider().apply { gate = CompletableDeferred() }
        val viewModel = CreatePostViewModel(repository, location)
        viewModel.onTextChange("テスト投稿")

        viewModel.submit()
        assertTrue(viewModel.uiState.value.isLocating)
        viewModel.submit()
        location.gate?.complete(Unit)

        assertEquals(1, location.calls)
        assertEquals(1, repository.createdLocations.size)
    }

    @Test
    fun onPostedHandled_resetsForm() {
        val viewModel = CreatePostViewModel(TestPostRepository(), TestLocationProvider())
        viewModel.onTextChange("テスト投稿")
        viewModel.submit()
        viewModel.onPostedHandled()
        assertFalse(viewModel.uiState.value.isPosted)
        assertEquals("", viewModel.uiState.value.text)
    }
}
