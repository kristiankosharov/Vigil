package app.vigil.common.ui.text

import app.vigil.common.core.AppError
import app.vigil.common.ui.R

/** The generic message for an error. Map to a feature string first when the screen needs a specific one. */
fun AppError.toUiText(): UiText = UiText.Resource(
    when (this) {
        is AppError.Network.NoConnection -> R.string.common_error_no_connection

        is AppError.Network.Timeout -> R.string.common_error_timeout

        is AppError.Network.Http -> R.string.common_error_server

        is AppError.Network.Serialization, is AppError.Network.Unknown -> R.string.common_error_unknown

        AppError.Local.NotFound -> R.string.common_error_not_found

        is AppError.Local.Constraint,
        is AppError.Local.Database,
        is AppError.Local.Storage,
        is AppError.Local.Unknown,
        -> R.string.common_error_storage

        is AppError.Unknown -> R.string.common_error_unknown
    },
)
