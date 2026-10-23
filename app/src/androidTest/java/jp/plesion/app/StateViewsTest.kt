package jp.plesion.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.plesion.app.core.ui.ErrorView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StateViewsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun errorView_showsMessageAndRetry() {
        composeRule.setContent { ErrorView(message = "エラー", onRetry = {}) }
        composeRule.onNodeWithText("エラー").assertExists()
        composeRule.onNodeWithText("再試行").assertExists()
    }
}
