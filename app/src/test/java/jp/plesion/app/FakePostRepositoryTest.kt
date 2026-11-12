package jp.plesion.app

import jp.plesion.app.data.local.FakePostRepository
import jp.plesion.app.data.local.SampleData
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePostRepositoryTest {
    private val center = GeoPoint(35.681236, 139.767125)

    @Test
    fun nearbyPosts_areSortedNewestFirst() = runTest {
        val posts = FakePostRepository().getNearbyPosts(center).getOrThrow()
        assertEquals(posts.sortedByDescending { it.createdAt }, posts)
    }

    @Test
    fun createPost_addsSampleMarkedPost() = runTest {
        val repository = FakePostRepository()
        val created = repository.createPost("hello", location = center).getOrThrow()

        assertTrue(created.id.startsWith(SampleData.ID_PREFIX))
        assertTrue(created.text.startsWith(SampleData.TEXT_PREFIX))
        assertTrue(repository.getNearbyPosts(center).getOrThrow().any { it.id == created.id })
    }

    @Test
    fun createPost_anonymous_hidesAuthorButIsStillMine() = runTest {
        val repository = FakePostRepository()
        val created = repository.createPost("hello", location = center, isAnonymous = true).getOrThrow()

        assertTrue(created.isAnonymous)
        assertEquals("", created.authorId)
        assertEquals(Post.ANONYMOUS_NAME, created.authorName)
        assertTrue(repository.isMyPost(created).getOrThrow())
    }

    @Test
    fun getMyPosts_includesOwnAnonymousPostsNewestFirst() = runTest {
        val repository = FakePostRepository()
        val named = repository.createPost("named", location = center).getOrThrow()
        val anonymous = repository.createPost("anonymous", location = center, isAnonymous = true).getOrThrow()

        val mine = repository.getMyPosts().getOrThrow()

        assertTrue(mine.any { it.id == named.id })
        assertTrue(mine.any { it.id == anonymous.id })
        assertTrue(mine.all { it.authorId == SampleData.me.id || it.isAnonymous })
        assertEquals(mine.sortedByDescending { it.createdAt }, mine)
    }

    @Test
    fun isMyPost_isFalseForOthersPosts() = runTest {
        val repository = FakePostRepository()
        val others = SampleData.posts.first { it.authorId != SampleData.me.id }
        val othersAnonymous = others.copy(id = "other-anonymous", authorId = "", isAnonymous = true)

        assertFalse(repository.isMyPost(others).getOrThrow())
        assertFalse(repository.isMyPost(othersAnonymous).getOrThrow())
    }

    @Test
    fun createPost_failsWhenBlank() = runTest {
        assertTrue(FakePostRepository().createPost("  ", location = null).isFailure)
    }

    @Test
    fun addComment_addsCommentAndIncrementsCount() = runTest {
        val repository = FakePostRepository()
        val post = SampleData.posts.first()

        val created = repository.addComment(post.id, "hello").getOrThrow()

        assertTrue(created.id.startsWith(SampleData.ID_PREFIX))
        assertTrue(created.text.startsWith(SampleData.TEXT_PREFIX))
        assertEquals(created, repository.getComments(post.id).getOrThrow().last())
        assertEquals(post.commentCount + 1, repository.getPost(post.id).getOrThrow().commentCount)
    }

    @Test
    fun addComment_failsWhenBlank() = runTest {
        assertTrue(FakePostRepository().addComment(SampleData.posts.first().id, "  ").isFailure)
    }

    @Test
    fun addComment_failsWhenPostNotFound() = runTest {
        assertTrue(FakePostRepository().addComment("missing", "hello").isFailure)
    }

    @Test
    fun sampleData_isClearlyMarked() {
        assertTrue(SampleData.posts.all { it.id.startsWith(SampleData.ID_PREFIX) })
        assertTrue(SampleData.posts.all { it.text.startsWith(SampleData.TEXT_PREFIX) })
    }

    @Test
    fun createPost_twiceInSameMillisecond_hasUniqueIds() = runTest {
        val repository = FakePostRepository()
        val ids = List(50) { repository.createPost("hello", location = null).getOrThrow().id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun addComment_twiceInSameMillisecond_hasUniqueIds() = runTest {
        val repository = FakePostRepository()
        val postId = SampleData.posts.first().id
        val ids = List(50) { repository.addComment(postId, "hello").getOrThrow().id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
