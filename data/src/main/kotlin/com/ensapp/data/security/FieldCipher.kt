package com.ensapp.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class FieldEncryptionException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

class FieldDecryptionException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

class FieldCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) {
    fun encrypt(plaintext: String, purpose: String): ByteArray {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key(createIfMissing = true))
            cipher.updateAAD(purpose.toByteArray(StandardCharsets.UTF_8))
            val encrypted = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            val nonce = cipher.iv
            return ByteBuffer.allocate(2 + nonce.size + encrypted.size)
                .put(FORMAT_VERSION)
                .put(nonce.size.toByte())
                .put(nonce)
                .put(encrypted)
                .array()
        } catch (exception: GeneralSecurityException) {
            throw FieldEncryptionException("No se pudo cifrar el campo sensible", exception)
        }
    }

    fun decrypt(payload: ByteArray, purpose: String): String {
        try {
            if (payload.size < HEADER_SIZE + GCM_NONCE_BYTES + GCM_TAG_BYTES) {
                throw FieldDecryptionException("El campo cifrado está truncado")
            }
            val buffer = ByteBuffer.wrap(payload)
            val version = buffer.get()
            if (version != FORMAT_VERSION) {
                throw FieldDecryptionException("Versión de cifrado no compatible")
            }
            val nonceLength = buffer.get().toInt() and 0xff
            if (nonceLength != GCM_NONCE_BYTES || buffer.remaining() < nonceLength + GCM_TAG_BYTES) {
                throw FieldDecryptionException("Formato de campo cifrado no válido")
            }
            val nonce = ByteArray(nonceLength).also(buffer::get)
            val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key(createIfMissing = false),
                GCMParameterSpec(GCM_TAG_BITS, nonce),
            )
            cipher.updateAAD(purpose.toByteArray(StandardCharsets.UTF_8))
            return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        } catch (exception: FieldDecryptionException) {
            throw exception
        } catch (exception: AEADBadTagException) {
            throw FieldDecryptionException("No se pudo autenticar el campo cifrado", exception)
        } catch (exception: GeneralSecurityException) {
            throw FieldDecryptionException("No se pudo descifrar el campo sensible", exception)
        }
    }

    private fun key(createIfMissing: Boolean): SecretKey = synchronized(keyLock) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return@synchronized it }
        if (!createIfMissing) {
            throw FieldDecryptionException("La clave de cifrado no está disponible")
        }

        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val DEFAULT_KEY_ALIAS = "ensapp_sensitive_fields_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val FORMAT_VERSION: Byte = 1
        const val HEADER_SIZE = 2
        const val GCM_NONCE_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        val keyLock = Any()
    }
}
