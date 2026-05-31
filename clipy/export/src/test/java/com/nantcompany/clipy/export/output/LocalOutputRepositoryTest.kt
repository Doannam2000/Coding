package com.nantcompany.clipy.export.output

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class LocalOutputRepositoryTest {

    private lateinit var repository: LocalOutputRepository

    @Before
    fun setup() {
        LocalOutputRepository.storageDir = createTempDir(prefix = "clipy-output-test-")
        repository = LocalOutputRepository()
        repository.clear()
    }

    @After
    fun tearDown() {
        repository.clear()
        LocalOutputRepository.storageDir?.deleteRecursively()
        LocalOutputRepository.storageDir = null
    }

    @Test
    fun `save should persist output to disk and getAll should retrieve it`() {
        val output = OutputMedia(
            id = "test-id",
            fileName = "test.mp4",
            path = "/tmp/test.mp4",
            sizeInBytes = 1024L,
            operation = "cut"
        )

        repository.save(output)
        val all = repository.getAll()

        assertTrue(all.isNotEmpty())
        val retrieved = all.first()
        assertEquals(output.id, retrieved.id)
        assertEquals(output.fileName, retrieved.fileName)
        assertEquals(output.path, retrieved.path)
        assertEquals(output.sizeInBytes, retrieved.sizeInBytes)
        assertEquals(output.operation, retrieved.operation)
    }

    @Test
    fun `clear should remove all records`() {
        val output = OutputMedia("id", "file", "/path", 0L)
        repository.save(output)
        assertEquals(1, repository.getAll().size)

        repository.clear()
        assertEquals(0, repository.getAll().size)
    }
}
