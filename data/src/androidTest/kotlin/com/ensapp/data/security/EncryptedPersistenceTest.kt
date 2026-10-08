package com.ensapp.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyStore
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedPersistenceTest {
    @Test
    fun ciphertext_is_authenticated_purpose_bound_and_requires_original_keystore_key() {
        val alias = "encrypted-test-${UUID.randomUUID()}"
        val cipher = FieldCipher(alias)
        val payload = cipher.encrypt("dato confidencial", "system.name")

        assertFalse(payload.toString(Charsets.UTF_8).contains("dato confidencial"))
        assertEquals("dato confidencial", cipher.decrypt(payload, "system.name"))
        assertThrows(FieldDecryptionException::class.java) {
            cipher.decrypt(payload, "response.note")
        }
        val corrupted = payload.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertThrows(FieldDecryptionException::class.java) {
            cipher.decrypt(corrupted, "system.name")
        }

        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            deleteEntry(alias)
        }
        assertThrows(FieldDecryptionException::class.java) {
            cipher.decrypt(payload, "system.name")
        }
    }
}
