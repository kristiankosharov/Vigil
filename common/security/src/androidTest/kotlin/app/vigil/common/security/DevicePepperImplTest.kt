package app.vigil.common.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevicePepperImplTest {

    @Test
    fun theKeystoreHmacIsStableAndKeyed() {
        val pepper = DevicePepperImpl()
        val input = ByteArray(32) { it.toByte() }

        val first = pepper.mac(input)

        assertEquals(32, first.size)
        assertArrayEquals(first, DevicePepperImpl().mac(input)) // the same Keystore key next time
        assertFalse(first.contentEquals(pepper.mac(ByteArray(32))))
    }
}
