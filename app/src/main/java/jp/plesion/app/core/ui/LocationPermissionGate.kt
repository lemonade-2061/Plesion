package jp.plesion.app.core.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** 位置情報を使えるかどうか。FINE が必須なので、COARSE だけの許可は使えない扱い（docs/DATA_CONTRACT.md §3）。 */
enum class LocationAccess { GRANTED, COARSE_ONLY, DENIED, SERVICE_OFF }

internal fun locationAccessOf(fine: Boolean, coarse: Boolean, serviceEnabled: Boolean): LocationAccess = when {
    fine && !serviceEnabled -> LocationAccess.SERVICE_OFF
    fine -> LocationAccess.GRANTED
    coarse -> LocationAccess.COARSE_ONLY
    else -> LocationAccess.DENIED
}

private fun Context.currentLocationAccess(): LocationAccess {
    fun granted(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
    val locationManager = getSystemService(LocationManager::class.java)
    return locationAccessOf(
        fine = granted(Manifest.permission.ACCESS_FINE_LOCATION),
        coarse = granted(Manifest.permission.ACCESS_COARSE_LOCATION),
        serviceEnabled = locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager),
    )
}

/**
 * 正確な位置情報が使えるときだけ [content] を表示し、使えないときは案内を表示する。
 * 設定アプリから戻ったときに権限・位置情報サービスの変更を反映するため、画面復帰（ON_RESUME）ごとに確認し直す。
 */
@Composable
fun LocationPermissionGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(context.currentLocationAccess()) }
    LifecycleResumeEffect(Unit) {
        access = context.currentLocationAccess()
        onPauseOrDispose {}
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        access = context.currentLocationAccess()
    }

    when (access) {
        LocationAccess.GRANTED -> content()
        LocationAccess.SERVICE_OFF -> LocationGuide(
            message = "端末の位置情報がOFFになっています。\n近くの投稿の表示と投稿には位置情報をONにしてください。",
            primaryLabel = "位置情報の設定を開く",
            onPrimary = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            modifier = modifier,
        )
        LocationAccess.COARSE_ONLY, LocationAccess.DENIED -> LocationGuide(
            message = if (access == LocationAccess.COARSE_ONLY) {
                "おおよその位置情報だけが許可されています。\n1km以内の投稿を正しく表示するため、「正確な位置情報」を許可してください。"
            } else {
                "近くの投稿の表示と投稿には、位置情報の許可が必要です。"
            },
            primaryLabel = "位置情報を許可する",
            onPrimary = {
                // Android 12 以降の仕様に合わせて FINE と COARSE をまとめて要求する
                launcher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            },
            // 「今後表示しない」の後はダイアログが出ないため、設定アプリからの許可も選べるようにする
            onOpenAppSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun LocationGuide(
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAppSettings: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, textAlign = TextAlign.Center)
        Button(onClick = onPrimary) { Text(primaryLabel) }
        if (onOpenAppSettings != null) {
            // 強調色の文字は紫の背景だと読みにくいので、文字は背景に合わせた色にする
            OutlinedButton(
                onClick = onOpenAppSettings,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
            ) { Text("アプリの設定を開く") }
        }
    }
}
