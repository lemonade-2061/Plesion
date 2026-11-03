package jp.plesion.app

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.plesion.app.core.location.FusedLocationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 実機・エミュレータで現在地が取れるかの確認。
 * 事前に位置を設定しておくこと（エミュレータなら Extended controls → Location、または `adb emu geo fix <経度> <緯度>`）。
 */
@RunWith(AndroidJUnit4::class)
class FusedLocationProviderDeviceTest {
    @Test
    fun getCurrentLocation_returnsLocation_whenPermissionGranted() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)

        // バックグラウンドのアプリには位置が返らない（"inactive"）ので、画面を前面に出した状態で取得する
        ActivityScenario.launch(MainActivity::class.java).use {
            val result = runBlocking { FusedLocationProvider(context).getCurrentLocation() }

            assertTrue("取得失敗: ${result.exceptionOrNull()}", result.isSuccess)
        }
    }
}
