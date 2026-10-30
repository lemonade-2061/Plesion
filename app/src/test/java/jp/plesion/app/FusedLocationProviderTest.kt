package jp.plesion.app

import jp.plesion.app.core.location.FusedLocationProvider
import jp.plesion.app.core.location.LocationPermissionDeniedException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class FusedLocationProviderTest {
    @Test
    fun getCurrentLocation_failsWithoutCrash_whenPermissionDenied() = runTest {
        val provider = FusedLocationProvider(
            hasPermission = { false },
            client = { error("権限がないときは位置情報クライアントを使わない") },
        )

        val result = provider.getCurrentLocation()

        assertTrue(result.exceptionOrNull() is LocationPermissionDeniedException)
    }
}
