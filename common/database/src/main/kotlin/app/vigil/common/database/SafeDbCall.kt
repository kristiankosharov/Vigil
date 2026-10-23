package app.vigil.common.database

import android.database.SQLException
import android.database.sqlite.SQLiteConstraintException
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.core.asRetryingResult
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

private const val TAG = "Database"

/** SQLite's primary result code for constraint violations; extended codes keep it in the low byte. */
private const val SQLITE_CONSTRAINT = 19
private const val PRIMARY_RESULT_CODE_MASK = 0xFF

/** The bundled SQLite driver reports failures as "Error code: <code>, message: <message>". */
private val DRIVER_ERROR_CODE = Regex("""Error code: (\d+)""")

/** Runs a one-shot Room call. Every failure becomes an [AppError.Local]; cancellation is rethrown. */
@Suppress("TooGenericExceptionCaught") // every failure becomes an AppError; cancellation is rethrown
suspend inline fun <T> safeDbCall(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(e.toLocalError())
    }

/** An observed Room flow as results: failures are emitted and the query is re-subscribed (`asRetryingResult`). */
fun <T> Flow<T>.asDbResult(): Flow<AppResult<T>> = asRetryingResult(mapError = { it.toLocalError() })

/** Maps a failed Room call to an [AppError.Local] and logs it (the wrappers are the only place errors are logged). */
fun Exception.toLocalError(): AppError.Local {
    val error = when {
        this is SQLException && isConstraintViolation() -> AppError.Local.Constraint(this)
        this is SQLException -> AppError.Local.Database(this)
        else -> AppError.Local.Unknown(this)
    }
    if (error is AppError.Local.Unknown) {
        Timber.tag(TAG).e(this, "Unexpected database error")
    } else {
        Timber.tag(TAG).w(this, "Database call failed: %s", error::class.simpleName)
    }
    return error
}

// androidx.sqlite's SQLiteException is android.database.SQLException on Android; the bundled driver
// puts the SQLite result code in the message instead of throwing SQLiteConstraintException.
private fun SQLException.isConstraintViolation(): Boolean = this is SQLiteConstraintException ||
    DRIVER_ERROR_CODE.find(message.orEmpty())?.groupValues?.get(1)?.toIntOrNull()
        ?.and(PRIMARY_RESULT_CODE_MASK) == SQLITE_CONSTRAINT
