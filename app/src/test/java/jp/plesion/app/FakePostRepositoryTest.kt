package jp.plesion.app

import jp.plesion.app.data.local.FakePostRepository
import jp.plesion.app.data.local.SampleData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePostRepositoryTest {
    @Test
    fun timeline_isSortedNewestFirst() = runTest {
        val posts = FakePostRepository().observeTimeline().first()
        assertEquals(posts.sortedByDescending { it.createdAt }, posts)
    }

    @Test
    fun createPost_addsSampleMarkedPost() = runTest {
        val repository = FakePostRepository()
        val created = repository.createPost("hello", location = null).getOrThrow()

        assertTrue(created.id.startsWith(SampleData.ID_PREFIX))
        assertTrue(created.text.startsWith(SampleData.TEXT_PREFIX))
        assertTrue(repository.observeTimeline().first().any { it.id == created.id })
    }

    @Test
    fun createPost_failsWhenBlank() = runTest {
        assertTrue(FakePostRepository().createPost("  ", location = null).isFailure)
    }

    @Test
    fun sampleData_isClearlyMarked() {
        assertTrue(SampleData.posts.all { it.id.startsWith(SampleData.ID_PREFIX) })
        assertTrue(SampleData.posts.all { it.text.startsWith(SampleData.TEXT_PREFIX) })
    }
}
