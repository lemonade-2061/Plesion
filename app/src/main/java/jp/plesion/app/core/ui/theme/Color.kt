package jp.plesion.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// アプリ全体で使う色。画面ごとに Color(0x...) を書かず、ここから使う。

/** 画面の背景。 */
val BgPurple = Color(0xFF42145F)

/** 投稿カード・入力欄など、背景の上に乗せる面。 */
val CardPurple = Color(0xFFA060B0)

/** アバターの背景・控えめなボタン。 */
val AvatarPurple = Color(0xFF7B3A8C)

/** 強調色（主ボタン・選択中・区切り線）。 */
val Accent = Color(0xFFC72FE0)

/** ダイアログのボタンなど、少し明るい面の上に置く強調色の文字。Accent だと暗くて読みにくい。 */
val AccentLight = Color(0xFFF0B8FF)

/** 主ボタンのグラデーションの終わり側。 */
val AccentDark = Color(0xFF9B1FC0)

/** 紫の背景の上で読めるエラー文字色。 */
val ErrorText = Color(0xFFFFB4AB)

/** コメントの吹き出し。 */
val CommentGray = Color(0xFFD9D9D9)

// Material コンポーネント（ナビゲーションバー・ダイアログなど）用の、背景より少し明るい面
internal val SurfaceContainerLowest = Color(0xFF370F50)
internal val SurfaceContainerLow = Color(0xFF3D1258)
internal val SurfaceContainer = Color(0xFF4A1A6A)
internal val SurfaceContainerHigh = Color(0xFF552277)
internal val SurfaceContainerHighest = Color(0xFF602A84)
