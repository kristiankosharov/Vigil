package app.vigil.common.database.documents

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    /** Newest first. */
    @Query("SELECT * FROM documents_document ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Upsert
    suspend fun upsert(document: DocumentEntity)
}
