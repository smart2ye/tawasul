package com.example.crypto

import android.content.Context
import android.util.Base64
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {
    private const val PREFS_NAME = "tawasul_crypto_prefs"
    private const val KEY_PRIVATE = "private_key"
    private const val KEY_PUBLIC = "public_key"
    private const val RSA_ALGO = "RSA/ECB/PKCS1Padding"
    private const val AES_ALGO = "AES/CBC/PKCS5Padding"

    private var myPrivateKey: PrivateKey? = null
    private var myPublicKeyString: String = ""

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPrivate = prefs.getString(KEY_PRIVATE, null)
        val savedPublic = prefs.getString(KEY_PUBLIC, null)

        if (savedPrivate != null && savedPublic != null) {
            try {
                val privateKeyBytes = Base64.decode(savedPrivate, Base64.DEFAULT)
                val publicKeyBytes = Base64.decode(savedPublic, Base64.DEFAULT)
                
                val keyFactory = KeyFactory.getInstance("RSA")
                val privateSpec = java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes)
                myPrivateKey = keyFactory.generatePrivate(privateSpec)
                myPublicKeyString = savedPublic
            } catch (e: Exception) {
                e.printStackTrace()
                generateAndSaveKeys(context)
            }
        } else {
            generateAndSaveKeys(context)
        }
    }

    private fun generateAndSaveKeys(context: Context) {
        try {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val pair = keyGen.generateKeyPair()
            
            myPrivateKey = pair.private
            val publicKeyBytes = pair.public.encoded
            myPublicKeyString = Base64.encodeToString(publicKeyBytes, Base64.DEFAULT)
            
            val privateKeyBytes = pair.private.encoded
            val privateKeyStr = Base64.encodeToString(privateKeyBytes, Base64.DEFAULT)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_PRIVATE, privateKeyStr)
                .putString(KEY_PUBLIC, myPublicKeyString)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMyPublicKey(): String = myPublicKeyString

    fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    fun encryptAES(plainText: String, secretKey: SecretKey): Pair<String, String> {
        val cipher = Cipher.getInstance(AES_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val ivBytes = cipher.iv
        
        val encryptedStr = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val ivStr = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
        return Pair(encryptedStr, ivStr)
    }

    fun decryptAES(encryptedBase64: String, ivBase64: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(AES_ALGO)
        val ivSpec = IvParameterSpec(Base64.decode(ivBase64, Base64.NO_WRAP))
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipher.doFinal(Base64.decode(encryptedBase64, Base64.NO_WRAP))
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun encryptAESKeyWithRSA(aesKey: SecretKey, peerPublicKeyBase64: String): String {
        val keyBytes = Base64.decode(peerPublicKeyBase64, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(keySpec)

        val cipher = Cipher.getInstance(RSA_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedKeyBytes = cipher.doFinal(aesKey.encoded)
        return Base64.encodeToString(encryptedKeyBytes, Base64.NO_WRAP)
    }

    fun decryptAESKeyWithRSA(encryptedAESKeyBase64: String): SecretKey? {
        val privateKey = myPrivateKey ?: return null
        val cipher = Cipher.getInstance(RSA_ALGO)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decryptedKeyBytes = cipher.doFinal(Base64.decode(encryptedAESKeyBase64, Base64.NO_WRAP))
        return SecretKeySpec(decryptedKeyBytes, "AES")
    }
}
