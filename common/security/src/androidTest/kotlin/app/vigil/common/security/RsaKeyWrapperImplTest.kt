package app.vigil.common.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.GeneralSecurityException
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RsaKeyWrapperImplTest {
    private val wrapper = RsaKeyWrapperImpl()

    @After
    fun tearDown() {
        wrapper.delete(ALIAS)
        wrapper.delete(OTHER)
    }

    @Test
    fun aWrappedKeyUnwrapsWithTheSameKeyPairOnly() {
        val key = AesGcm.newKey()

        val wrapped = wrapper.wrap(ALIAS, key)

        assertArrayEquals(key, wrapper.unwrap(ALIAS, wrapped))
        wrapper.wrap(OTHER, key) // creates the other key pair
        assertThrows(GeneralSecurityException::class.java) { wrapper.unwrap(OTHER, wrapped) }
    }

    @Test
    fun afterDeleteNothingUnwraps() {
        val wrapped = wrapper.wrap(ALIAS, AesGcm.newKey())

        wrapper.delete(ALIAS)

        assertThrows(GeneralSecurityException::class.java) { wrapper.unwrap(ALIAS, wrapped) }
    }

    private companion object {
        const val ALIAS = "test_rsa_wrapper"
        const val OTHER = "test_rsa_wrapper_other"
    }
}
