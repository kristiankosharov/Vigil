package app.vigil.feature.applock.domain.model

import kotlin.time.Duration

public sealed interface PinCheck {
    public data object Correct : PinCheck

    /** [attemptsLeft] before everything is erased; [retryAfter] until the next PIN can be tried. */
    public data class Wrong(val attemptsLeft: Int, val retryAfter: Duration) : PinCheck

    /** That was the last attempt: everything is erased and the user is signed out. */
    public data object Erased : PinCheck
}
