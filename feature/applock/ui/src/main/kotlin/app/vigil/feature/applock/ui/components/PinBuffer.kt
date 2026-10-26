package app.vigil.feature.applock.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import app.vigil.feature.applock.domain.model.PinRules

/**
 * The digits typed so far, in a CharArray that gets wiped; never a String, which can't be. Only
 * [length] is observable state, and nothing here is ever saved (no savedStateHandle).
 */
internal class PinBuffer {
    private val digits = CharArray(PinRules.LENGTH)

    var length by mutableIntStateOf(0)
        private set

    val isFull: Boolean get() = length == PinRules.LENGTH

    fun add(digit: Int) {
        if (isFull || digit !in 0..<RADIX) return
        digits[length] = Character.forDigit(digit, RADIX)
        length++
    }

    fun delete() {
        if (length == 0) return
        length--
        digits[length] = Char.MIN_VALUE
    }

    fun clear() {
        digits.fill(Char.MIN_VALUE)
        length = 0
    }

    /** A copy for one check or save; the caller wipes it. */
    fun copy(): CharArray = digits.copyOf(length)

    fun contentEquals(other: PinBuffer): Boolean =
        length == other.length && (0 until length).all { digits[it] == other.digits[it] }

    private companion object {
        const val RADIX = 10
    }
}
