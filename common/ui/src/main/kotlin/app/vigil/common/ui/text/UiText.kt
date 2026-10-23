package app.vigil.common.ui.text

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * User-facing text as plain data, resolved only when shown. Never holds a Context, Resources, View
 * or lambda: UiText lives in ViewModel state and effects.
 */
@Immutable
sealed interface UiText {
    data class Dynamic(val value: String) : UiText

    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText
}

@Suppress("SpreadOperator") // the string APIs take varargs; UiText has a handful of args at most
fun UiText.asString(context: Context): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> context.getString(id, *args.toTypedArray())
    is UiText.Plural -> context.resources.getQuantityString(id, count, *args.toTypedArray())
}

@Composable
@Suppress("SpreadOperator") // the string APIs take varargs; UiText has a handful of args at most
fun UiText.asString(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
    is UiText.Plural -> pluralStringResource(id, count, *args.toTypedArray())
}
