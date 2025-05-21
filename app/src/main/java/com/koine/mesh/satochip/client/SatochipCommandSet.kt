package com.koine.mesh.satochip.client

import java.io.IOException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class SatochipCommandSet(private val cardChannel: CardChannel) {
    private val secureRandom = SecureRandom()
    private var secureChannelSession: SecureChannelSession? = null
    
    data class CardResponse(
        val data: ByteArray,
        val sw1: Byte,
        val sw2: Byte
    ) {
        val statusWord: Int
            get() = ((sw1.toInt() and 0xFF) shl 8) or (sw2.toInt() and 0xFF)
            
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as CardResponse
            return data.contentEquals(other.data) && sw1 == other.sw1 && sw2 == other.sw2
        }
        
        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + sw1
            result = 31 * result + sw2
            return result
        }
    }
    
    fun selectApplet(): CardResponse {
        val aid = byteArrayOf(
            0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x62.toByte(),
            0x03.toByte(), 0x01.toByte(), 0x0C.toByte(), 0x06.toByte(), 0x01.toByte()
        )
        return transmit(CommandAPDU(
            Constants.CLA_ISO7816,
            Constants.INS_SELECT,
            Constants.P1_SELECT_BY_NAME,
            Constants.P2_SELECT_FIRST_OR_ONLY,
            aid
        ))
    }
    
    fun getStatus(): CardResponse {
        return transmit(CommandAPDU(
            Constants.CLA_SATOCHIP,
            Constants.INS_GET_STATUS,
            0x00.toByte(),
            0x00.toByte()
        ))
    }
    
    fun verifyPin(pin: String): CardResponse {
        val pinBytes = pin.toByteArray()
        return transmit(CommandAPDU(
            Constants.CLA_SATOCHIP,
            Constants.INS_VERIFY_PIN,
            0x00.toByte(),
            0x00.toByte(),
            pinBytes
        ))
    }
    
    fun changePin(oldPin: String, newPin: String): CardResponse {
        val data = ByteArray(oldPin.length + newPin.length)
        System.arraycopy(oldPin.toByteArray(), 0, data, 0, oldPin.length)
        System.arraycopy(newPin.toByteArray(), 0, data, oldPin.length, newPin.length)
        
        return transmit(CommandAPDU(
            Constants.CLA_SATOCHIP,
            Constants.INS_CHANGE_PIN,
            0x00.toByte(),
            0x00.toByte(),
            data
        ))
    }
    
    fun loadKey(keyType: Byte, keyData: ByteArray): CardResponse {
        return transmit(CommandAPDU(
            Constants.CLA_SATOCHIP,
            Constants.INS_LOAD_KEY,
            keyType,
            0x00.toByte(),
            keyData
        ))
    }
    
    fun deriveKey(keyType: Byte, path: ByteArray): CardResponse {
        return transmit(CommandAPDU(
            Constants.CLA_SATOCHIP,
            Constants.INS_DERIVE_KEY,
            keyType,
            0x00.toByte(),
            path
        ))
    }
    
    fun sign(data: ByteArray): CardResponse {
        return transmit(CommandAPDU(
            Constants.CLA_SATOCHIP,
            Constants.INS_SIGN,
            0x00.toByte(),
            0x00.toByte(),
            data
        ))
    }
    
    fun getPublicKey(keyType: Byte): CardResponse {
        return transmit(CommandAPDU(
            Constants.CLA_SATOCHIP,
            Constants.INS_GET_PUBKEY,
            keyType,
            0x00.toByte()
        ))
    }
    
    private fun transmit(command: CommandAPDU): CardResponse {
        try {
            val response = cardChannel.transmit(command)
            return CardResponse(
                response.data,
                response.sw1,
                response.sw2
            )
        } catch (e: IOException) {
            throw SatochipException("Failed to transmit command: ${e.message}", e)
        }
    }
    
    fun establishSecureChannel(): Boolean {
        // Generate random challenge
        val challenge = ByteArray(16)
        secureRandom.nextBytes(challenge)
        
        // Send challenge to card
        val response = transmit(CommandAPDU(
            Constants.CLA_SATOCHIP,
            0x10.toByte(), // INS_ESTABLISH_SECURE_CHANNEL
            0x00.toByte(),
            0x00.toByte(),
            challenge
        ))
        
        if (response.statusWord != Constants.SW_SUCCESS) {
            return false
        }
        
        // Process card response and establish secure channel
        secureChannelSession = SecureChannelSession(response.data)
        return true
    }
    
    fun encryptData(data: ByteArray): ByteArray {
        return secureChannelSession?.encrypt(data) ?: throw SatochipException("Secure channel not established")
    }
    
    fun decryptData(data: ByteArray): ByteArray {
        return secureChannelSession?.decrypt(data) ?: throw SatochipException("Secure channel not established")
    }
}

class SatochipException(message: String, cause: Throwable? = null) : Exception(message, cause)

class SecureChannelSession(private val sessionKey: ByteArray) {
    private val cipher = Cipher.getInstance("AES/CBC/NoPadding")
    private val key = SecretKeySpec(sessionKey, "AES")
    
    fun encrypt(data: ByteArray): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(data)
    }
    
    fun decrypt(data: ByteArray): ByteArray {
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(data)
    }
} 