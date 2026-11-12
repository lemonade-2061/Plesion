package jp.plesion.app

import jp.plesion.app.core.ui.UiState
import jp.plesion.app.data.local.FakeAuthRepository
import jp.plesion.app.data.local.FakePostRepository
import jp.plesion.app.data.local.FakeUserRepository
import jp.plesion.app.data.local.SampleData
import jp.plesion.app.feature.profile.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_showsSignedInUser() = runTest {
        val auth = FakeAuthRepository().apply { signInWithGoogle("token") }
        val viewModel = ProfileViewModel(FakeUserRepository(), auth, FakePostRepository())
        assertEquals(UiState.Success(SampleData.me), viewModel.uiState.value)
    }

    @Test
    fun load_showsMyPostsOnly() = runTest {
        val auth = FakeAuthRepository().apply { signInWithGoogle("token") }
        val viewModel = ProfileViewModel(FakeUserRepository(), auth, FakePostRepository())
        val myPostIds = SampleData.posts.filter { it.authorId == SampleData.me.id }.map { it.id }
        assertEquals(myPostIds, viewModel.posts.value?.map { it.id })
    }

    @Test
    fun load_notSignedIn_showsError() {
        val viewModel = ProfileViewModel(FakeUserRepository(), FakeAuthRepository(), FakePostRepository())
        assertTrue(viewModel.uiState.value is UiState.Error)
    }
}
