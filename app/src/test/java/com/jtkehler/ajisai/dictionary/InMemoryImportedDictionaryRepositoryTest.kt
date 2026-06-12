package com.jtkehler.ajisai.dictionary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryImportedDictionaryRepositoryTest {
    @Test
    fun repositoryStartsEmpty() {
        val repository = InMemoryImportedDictionaryRepository()

        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun repositoryReturnsDictionariesInPriorityOrder() {
        val repository = InMemoryImportedDictionaryRepository()
        repository.save(dictionary(id = "second", priority = 1))
        repository.save(dictionary(id = "first", priority = 0))

        assertEquals(listOf("first", "second"), repository.getAll().map { it.id })
    }

    private fun dictionary(id: String, priority: Int) = ImportedDictionary(
        id = id,
        title = id,
        type = DictionaryType.TERM,
        priority = priority,
        importedAtMs = 0L,
    )
}
