package jp.plesion.app.feature.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import jp.plesion.app.core.ui.LocationPermissionGate
import jp.plesion.app.core.ui.theme.Accent
import jp.plesion.app.core.ui.theme.AccentDark
import jp.plesion.app.core.ui.theme.AvatarPurple
import jp.plesion.app.core.ui.theme.BgPurple
import jp.plesion.app.core.ui.theme.CardPurple
import jp.plesion.app.core.ui.theme.ErrorText
import jp.plesion.app.data.model.Post

/**
 * カテゴリー・終了日時は、保存先（PostRepository.createPost）がまだ対応していない。
 * 押せるのに保存されない状態をユーザーに見せないよう、対応するまでは false にして隠す。
 * 対応したら true にして、ViewModel と合わせて実際の処理につなぐ。
 */
private const val SHOW_UNSUPPORTED_SECTIONS = false

private val categories = listOf("おでかけ", "雑談", "ごはん", "スポーツ", "その他")

@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onPosted: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Photo Picker は権限なしで使える。上限を超えて選ばれた分は ViewModel が捨てる
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(Post.MAX_IMAGES),
    ) { uris -> viewModel.onImagesPicked(uris) }

    LaunchedEffect(state.isPosted) {
        if (state.isPosted) {
            viewModel.onPostedHandled()
            onPosted()
        }
    }

    // 正確な位置情報がないと投稿できないため、許可されるまでは入力欄の代わりに案内を出す
    LocationPermissionGate(modifier = modifier) {
        CreatePostContent(
            text = state.text,
            isAnonymous = state.isAnonymous,
            imageUris = state.imageUris,
            isLocating = state.isLocating,
            isSubmitting = state.isSubmitting,
            errorMessage = state.errorMessage,
            onTextChange = viewModel::onTextChange,
            onAnonymousChange = viewModel::onAnonymousChange,
            onPickImages = {
                pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onImageRemoved = viewModel::onImageRemoved,
            onSubmit = viewModel::submit,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun CreatePostContent(
    text: String,
    isAnonymous: Boolean,
    imageUris: List<Uri>,
    isLocating: Boolean,
    isSubmitting: Boolean,
    errorMessage: String?,
    onTextChange: (String) -> Unit,
    onAnonymousChange: (Boolean) -> Unit,
    onPickImages: () -> Unit,
    onImageRemoved: (Uri) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    nickname: String? = null, // ユーザー情報を取れるようになったら渡す
) {
    // SHOW_UNSUPPORTED_SECTIONS が true のときだけ使う、見た目だけの状態
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .background(BgPurple)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // 上のバー
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "戻る",
                tint = Color.White,
                modifier = Modifier.clickable { onBack() },
            )
            Spacer(Modifier.width(16.dp))
            Text("投稿を作成", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(thickness = 2.dp, color = Accent.copy(alpha = 0.6f))

        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // 投稿者
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).background(AvatarPurple, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                if (nickname != null) {
                    Spacer(Modifier.width(12.dp))
                    Text(nickname, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "投稿者の表示方法",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                DisplayModeToggle(anonymous = isAnonymous, onChange = onAnonymousChange)
            }
            Spacer(Modifier.height(8.dp))

            // 本文
            Text(
                "いまどうしてる？",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Box {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardPurple,
                        unfocusedContainerColor = CardPurple,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White,
                    ),
                )
                Text(
                    "${text.length}/${MAX_POST_LENGTH}文字",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
                )
            }
            Spacer(Modifier.height(20.dp))

            // 画像
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("画像を追加", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "${imageUris.size} / ${Post.MAX_IMAGES} 枚",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                imageUris.forEach { uri ->
                    SelectedImage(uri, enabled = !isSubmitting, onRemove = { onImageRemoved(uri) }, modifier = Modifier.weight(1f))
                }
                // 空いている枠の 1 つ目だけ押せる（押すと Photo Picker を開く）
                repeat(Post.MAX_IMAGES - imageUris.size) { index ->
                    ImageSlot(
                        onClick = if (index == 0 && !isSubmitting) onPickImages else null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "× で選び直せます。写真の位置情報は投稿時に消えます",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
            )
            Spacer(Modifier.height(20.dp))

            if (SHOW_UNSUPPORTED_SECTIONS) {
                // カテゴリー
                Text("カテゴリー", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    categories.forEach { c ->
                        val selected = c == selectedCategory
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) Accent else CardPurple)
                                .clickable { selectedCategory = if (selected) null else c }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            Text(c, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "選択しなくても投稿できます",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(20.dp))

                // 投稿の終了日時
                Text("投稿の終了日時", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardPurple)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "日時を設定する（任意）",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.height(24.dp))
            }

            // 状態・エラー
            if (isLocating) {
                Text("現在地を取得しています…", color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }
            errorMessage?.let {
                Text(it, color = ErrorText, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
            }

            // ボタン
            SubmitButton(
                enabled = !isSubmitting && text.isNotBlank(),
                loading = isSubmitting,
                onClick = onSubmit,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AvatarPurple)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text("キャンセルして戻る", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DisplayModeToggle(anonymous: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(CardPurple.copy(alpha = 0.6f)),
    ) {
        ToggleSegment("名前", selected = !anonymous) { onChange(false) }
        ToggleSegment("匿名", selected = anonymous) { onChange(true) }
    }
}

@Composable
private fun ToggleSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Accent else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 5.dp),
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ImageSlot(onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(CardPurple.copy(alpha = if (onClick != null) 0.6f else 0.3f))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .drawBehind {
                drawRoundRect(
                    color = Accent.copy(alpha = 0.8f),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                    ),
                )
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Add, contentDescription = "画像を追加", tint = BgPurple, modifier = Modifier.size(28.dp))
        Text("追加", color = BgPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SelectedImage(uri: Uri, enabled: Boolean, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1f)) {
        AsyncImage(
            model = uri,
            contentDescription = "選んだ画像",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
        )
        if (enabled) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(role = Role.Button, onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, contentDescription = "画像を外す", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SubmitButton(enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled || loading) 1f else 0.5f
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(Accent.copy(alpha = alpha), AccentDark.copy(alpha = alpha))))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text("投稿する", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// プレビュー用の表示確認データ。アプリ本体の画面には出ない
@Preview(showBackground = true, widthDp = 360, heightDp = 700)
@Composable
private fun CreatePostContentPreview() {
    CreatePostContent(
        text = "",
        isAnonymous = false,
        imageUris = emptyList(),
        isLocating = false,
        isSubmitting = false,
        errorMessage = null,
        onTextChange = {},
        onAnonymousChange = {},
        onPickImages = {},
        onImageRemoved = {},
        onSubmit = {},
        onBack = {},
    )
}