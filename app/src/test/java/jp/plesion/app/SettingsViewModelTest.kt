package jp.plesion.app

import jp.plesion.app.data.local.FakeAuthRepository
import jp.plesion.app.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signOut_signsOutAndMarksDone() = runTest {
        val auth = FakeAuthRepository().apply { signInWithGoogle("token") }
        val viewModel = SettingsViewModel(auth)

        viewModel.signOut()

        assertNull(auth.currentUser.first())
        assertTrue(viewModel.isSignedOut.value)
    }
}
