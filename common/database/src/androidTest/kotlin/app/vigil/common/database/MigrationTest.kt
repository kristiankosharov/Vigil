package app.vigil.common.database

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.vigil.common.database.migrations.DropHomeEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val file = instrumentation.targetContext.getDatabasePath("migration-test.db")

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = file,
        driver = BundledSQLiteDriver(),
        databaseClass = AppDatabase::class,
        autoMigrationSpecs = listOf(DropHomeEntry()),
    )

    @Test
    fun migrate1To2DropsHomeEntryAndCreatesDocuments() = runBlocking {
        helper.createDatabase(1).apply {
            execSQL("INSERT INTO home_entry (id, title) VALUES ('1', 'Placeholder')")
            close()
        }

        val connection = helper.runMigrationsAndValidate(2) // also checks the schema against 2.json

        connection.prepare("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'home_entry'").use {
            it.step()
            assertEquals(0L, it.getLong(0))
        }
        connection.execSQL(
            "INSERT INTO documents_document (id, title, status, pageCount, addedAt, searchText) " +
                "VALUES ('1', 'Rental agreement', 'to_sign', 4, 0, '')",
        )
        connection.prepare("SELECT title FROM documents_document").use {
            it.step()
            assertEquals("Rental agreement", it.getText(0))
        }
        connection.close()
    }
}
