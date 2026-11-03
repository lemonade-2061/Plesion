package jp.plesion.app

import jp.plesion.app.core.location.LocationPermissionDeniedException
import jp.plesion.app.core.location.LocationUnavailableException
import jp.plesion.app.core.ui.UiState
import jp.plesion.app.data.local.FakePostRepository
import jp.plesion.app.data.local.SampleData
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.remote.filterNearby
import jp.plesion.app.data.repository.PostRepository
import jp.plesion.app.feature.feed.HomeViewModel
import kotlinx.coroutines.CompletableDeferred
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
class HomeViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun HomeViewModel.posts(): List<Post> = (uiState.value as UiState.Success).data

    @Test
    fun initialState_isLoading_untilRefresh() {
        val viewModel = HomeViewModel(TestPostRepository(), TestLocationProvider())
        assertEquals(UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun refresh_showsOnlyPostsWithin1km_newestFirst() {
        fun post(id: String, location: GeoPoint?, createdAt: Long) =
            Post(id = id, authorId = "a", authorName = "a", text = id, location = location, createdAt = createdAt)
        val posts = listOf(
            post("near-old", TOKYO_STATION, createdAt = 1),
            // 約 1.1km 北（範囲外）
            post("far", GeoPoint(TOKYO_STATION.latitude + 0.01, TOKYO_STATION.longitude), createdAt = 3),
            // 約 500m 北（範囲内・最新）
            post("near-new", GeoPoint(TOKYO_STATION.latitude + 0.0045, TOKYO_STATION.longitude), createdAt = 2),
            post("no-location", null, createdAt = 4),
        )
        val repository = object : PostRepository by FakePostRepository() {
            override suspend fun getNearbyPosts(center: GeoPoint, radiusMeters: Double) =
                Result.success(filterNearby(posts, center, radiusMeters))
        }
        val viewModel = HomeViewModel(repository, TestLocationProvider())

        viewModel.refresh()

        assertEquals(listOf("near-new", "near-old"), viewModel.posts().map { it.id })
    }

    @Test
    fun refresh_withFakeRepository_showsSamplePostsNearTokyoStation() {
        val viewModel = HomeViewModel(FakePostRepository(), TestLocationProvider())
        viewModel.refresh()
        assertEquals(SampleData.posts.map { it.id }.toSet(), viewModel.posts().map { it.id }.toSet())
    }

    @Test
    fun refresh_requestsRadiusOf1000mAroundCurrentLocation() {
        val repository = TestPostRepository()
        HomeViewModel(repository, TestLocationProvider()).refresh()
        assertEquals(listOf(TOKYO_STATION to 1_000.0), repository.nearbyCenters)
    }

    @Test
    fun refresh_farFromAllPosts_showsEmptyList() {
        val viewModel = HomeViewModel(TestPostRepository(), TestLocationProvider(Result.success(GeoPoint(43.0, 141.0))))
        viewModel.refresh()
        assertEquals(emptyList<Post>(), viewModel.posts())
    }

    @Test
    fun refresh_networkError_showsError_thenRetrySucceeds() {
        val repository = TestPostRepository().apply { nearbyFailure = RuntimeException("offline") }
        val viewModel = HomeViewModel(repository, TestLocationProvider())

        viewModel.refresh()
        assertTrue(viewModel.uiState.value is UiState.Error)

        repository.nearbyFailure = null
        viewModel.refresh()
        assertEquals(SampleData.posts.size, viewModel.posts().size)
    }

    @Test
    fun refresh_permissionDenied_showsErrorWithoutQuerying() {
        val repository = TestPostRepository()
        val viewModel = HomeViewModel(repository, TestLocationProvider(Result.failure(LocationPermissionDeniedException())))
        viewModel.refresh()
        assertTrue((viewModel.uiState.value as UiState.Error).message.contains("許可"))
        assertTrue(repository.nearbyCenters.isEmpty())
    }

    @Test
    fun refresh_locationUnavailable_showsError_thenRetrySucceeds() {
        val location = TestLocationProvider(Result.failure(LocationUnavailableException()))
        val viewModel = HomeViewModel(TestPostRepository(), location)

        viewModel.refresh()
        assertTrue((viewModel.uiState.value as UiState.Error).message.contains("位置情報"))

        location.result = Result.success(TOKYO_STATION)
        viewModel.refresh()
        assertTrue(viewModel.uiState.value is UiState.Success)
    }

    @Test
    fun refresh_whileLoading_doesNotStartSecondRequest() {
        val location = TestLocationProvider().apply { gate = CompletableDeferred() }
        val viewModel = HomeViewModel(TestPostRepository(), location)

        viewModel.refresh()
        viewModel.refresh()
        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)
        location.gate?.complete(Unit)

        assertEquals(1, location.calls)
        assertTrue(!viewModel.isRefreshing.value)
    }

    @Test
    fun refresh_keepsPreviousListWhileReloading() {
        val location = TestLocationProvider()
        val viewModel = HomeViewModel(TestPostRepository(), location)
        viewModel.refresh()

        location.gate = CompletableDeferred()
        viewModel.refresh()
        assertTrue(viewModel.uiState.value is UiState.Success)
        location.gate?.complete(Unit)
    }

    @Test
    fun refresh_afterCreatingPost_showsNewPostFirst() = runTest {
        val repository = TestPostRepository()
        val viewModel = HomeViewModel(repository, TestLocationProvider())
        viewModel.refresh()

        val created = repository.createPost("new", TOKYO_STATION).getOrThrow()
        viewModel.refresh()

        assertEquals(created.id, viewModel.posts().first().id)
    }
}
