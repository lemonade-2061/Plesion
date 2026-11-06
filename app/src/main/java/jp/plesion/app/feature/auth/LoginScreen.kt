package jp.plesion.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.plesion.app.data.repository.GoogleIdTokenProvider

private val BgPurple = Color(0xFF42145F)
private val Accent = Color(0xFFC72FE0)
private val AccentDark = Color(0xFF9B1FC0)
private val ErrorText = Color(0xFFFFB4AB)

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    googleIdTokenProvider: GoogleIdTokenProvider,
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    LoginContent(
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        onGoogleLoginClick = { viewModel.signIn { googleIdTokenProvider.getIdToken(context) } },
        modifier = modifier,
    )
}

@Composable
fun LoginContent(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPurple)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // タイトルとボタンを画面の中央に置く
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Plesion",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(48.dp))

            GoogleLoginButton(
                onClick = onGoogleLoginClick,
                isLoading = isLoading,
            )

            // エラー表示（キャンセル時は null なので何も出ない）
            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = ErrorText, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("利用規約", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
            Text("プライバシーポリシー", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GoogleLoginButton(
    onClick: () -> Unit,
    isLoading: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(Accent, AccentDark)))
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text("Googleでログイン", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// @Preview 以外でダミーデータは置かない決まりなので、プレビュー用だけここに書く
@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun LoginContentPreview() {
    LoginContent(
        isLoading = false,
        errorMessage = null,
        onGoogleLoginClick = {},
    )
}