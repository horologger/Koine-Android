package com.koine.mesh.satochip

import java.security.SecureRandom
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import java.security.MessageDigest

class SecureChannelSession {
    private var initialized = false
    private var sessionKey: ByteArray? = null
    private var iv: ByteArray? = null
    private var ivCounter = 0
    private var derivedKey: ByteArray? = null

    val isInitialized: Boolean
        get() = initialized

    fun encryptWithPublicKey(data: ByteArray, publicKeyBytes: ByteArray): ByteArray {
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val publicKey = keyFactory.generatePublic(keySpec)
        
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(data)
    }

    fun initialize(cardResponse: ByteArray, challenge: ByteArray) {
        // Derive session key from challenge
        val md = MessageDigest.getInstance("SHA-256")
        md.update(challenge)
        this.sessionKey = md.digest()
        
        // Initialize IV from card response
        this.iv = ByteArray(16)
        System.arraycopy(cardResponse, 0, this.iv!!, 0, 16)
        this.ivCounter = 0
        
        // Derive encryption key
        val keyMd = MessageDigest.getInstance("SHA-256")
        keyMd.update(this.sessionKey!!)
        this.derivedKey = keyMd.digest()
        
        this.initialized = true
    }

    fun encrypt(data: ByteArray): ByteArray {
        if (!initialized) throw IllegalStateException("Secure channel not initialized")
        
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(derivedKey!!, "AES")
        val ivSpec = IvParameterSpec(iv!!)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(data)
        
        // Compute MAC
        val mac = computeMac(encrypted)
        
        // Combine encrypted data and MAC
        val result = ByteArray(encrypted.size + mac.size)
        System.arraycopy(encrypted, 0, result, 0, encrypted.size)
        System.arraycopy(mac, 0, result, encrypted.size, mac.size)
        
        // Increment IV for next operation
        incrementIv()
        
        return result
    }

    fun decrypt(data: ByteArray): ByteArray {
        if (!initialized) throw IllegalStateException("Secure channel not initialized")
        
        // Split data into encrypted part and MAC
        val encrypted = data.copyOfRange(0, data.size - 20) // 20 bytes for SHA-1 MAC
        val receivedMac = data.copyOfRange(data.size - 20, data.size)
        
        // Verify MAC
        val computedMac = computeMac(encrypted)
        if (!computedMac.contentEquals(receivedMac)) {
            throw SecurityException("MAC verification failed")
        }
        
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(derivedKey!!, "AES")
        val ivSpec = IvParameterSpec(iv!!)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decrypted = cipher.doFinal(encrypted)
        
        // Increment IV for next operation
        incrementIv()
        
        return decrypted
    }

    fun computeMac(data: ByteArray): ByteArray {
        if (!initialized) throw IllegalStateException("Secure channel not initialized")
        
        val mac = Mac.getInstance("HmacSHA1")
        val keySpec = SecretKeySpec(derivedKey!!, "HmacSHA1")
        mac.init(keySpec)
        return mac.doFinal(data)
    }

    fun incrementIv() {
        ivCounter++
        // Update IV based on counter
        for (i in 0..3) {
            iv!![15 - i] = (ivCounter shr (i * 8)).toByte()
        }
    }
} 