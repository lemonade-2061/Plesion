package jp.plesion.app

import jp.plesion.app.data.local.FakeAuthRepository
import jp.plesion.app.data.repository.GoogleSignInCancelledException
import jp.plesion.app.feature.auth.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// viewModelScope は Dispatchers.Main を使うため、テストでは差し替える
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signIn_withToken_marksSignedIn() {
        val viewModel = LoginViewModel(FakeAuthRepository())
        viewModel.signIn { Result.success("token") }
        assertTrue(viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun signIn_whenCancelled_showsNoError() {
        val viewModel = LoginViewModel(FakeAuthRepository())
        viewModel.signIn { Result.failure(GoogleSignInCancelledException()) }

        assertFalse(viewModel.uiState.value.isSignedIn)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun signIn_whenTokenFails_showsError() {
        val viewModel = LoginViewModel(FakeAuthRepository())
        viewModel.signIn { Result.failure(IllegalStateException("failed")) }

        assertFalse(viewModel.uiState.value.isSignedIn)
        assertEquals("failed", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun init_whenAlreadySignedIn_marksSignedIn() = runTest {
        val repository = FakeAuthRepository()
        repository.signInWithGoogle("token")

        assertTrue(LoginViewModel(repository).uiState.value.isSignedIn)
    }
}
