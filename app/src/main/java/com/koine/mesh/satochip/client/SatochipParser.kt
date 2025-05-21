package com.koine.mesh.satochip.client

import java.nio.ByteBuffer
import java.nio.ByteOrder

class SatochipParser {
    data class ApplicationStatus(
        val pinRetryCount: Int,
        val pukRetryCount: Int,
        val isInitialized: Boolean,
        val isAuthenticated: Boolean,
        val hasMasterKey: Boolean,
        val hasAuthenticationKey: Boolean,
        val hasEncryptionKey: Boolean,
        val hasSignatureKey: Boolean
    )
    
    data class SatodimeStatus(
        val isInitialized: Boolean,
        val isAuthenticated: Boolean,
        val isActivated: Boolean,
        val isSpent: Boolean,
        val denomination: Int,
        val currency: String,
        val activationDate: Long,
        val expirationDate: Long
    )
    
    data class SatodimeKeyslotStatus(
        val isInitialized: Boolean,
        val isAuthenticated: Boolean,
        val isActivated: Boolean,
        val isSpent: Boolean,
        val denomination: Int,
        val currency: String,
        val activationDate: Long,
        val expirationDate: Long,
        val publicKey: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SatodimeKeyslotStatus
            return isInitialized == other.isInitialized &&
                   isAuthenticated == other.isAuthenticated &&
                   isActivated == other.isActivated &&
                   isSpent == other.isSpent &&
                   denomination == other.denomination &&
                   currency == other.currency &&
                   activationDate == other.activationDate &&
                   expirationDate == other.expirationDate &&
                   publicKey.contentEquals(other.publicKey)
        }
        
        override fun hashCode(): Int {
            var result = isInitialized.hashCode()
            result = 31 * result + isAuthenticated.hashCode()
            result = 31 * result + isActivated.hashCode()
            result = 31 * result + isSpent.hashCode()
            result = 31 * result + denomination
            result = 31 * result + currency.hashCode()
            result = 31 * result + activationDate.hashCode()
            result = 31 * result + expirationDate.hashCode()
            result = 31 * result + publicKey.contentHashCode()
            return result
        }
    }
    
    companion object {
        fun parseApplicationStatus(data: ByteArray): ApplicationStatus {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            
            val flags = buffer.get()
            val pinRetryCount = buffer.get().toInt() and 0xFF
            val pukRetryCount = buffer.get().toInt() and 0xFF
            
            return ApplicationStatus(
                pinRetryCount = pinRetryCount,
                pukRetryCount = pukRetryCount,
                isInitialized = (flags.toInt() and 0x01) != 0,
                isAuthenticated = (flags.toInt() and 0x02) != 0,
                hasMasterKey = (flags.toInt() and 0x04) != 0,
                hasAuthenticationKey = (flags.toInt() and 0x08) != 0,
                hasEncryptionKey = (flags.toInt() and 0x10) != 0,
                hasSignatureKey = (flags.toInt() and 0x20) != 0
            )
        }
        
        fun parseSatodimeStatus(data: ByteArray): SatodimeStatus {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            
            val flags = buffer.get()
            val denomination = buffer.get().toInt() and 0xFF
            val currencyLength = buffer.get().toInt() and 0xFF
            val currency = String(data, buffer.position(), currencyLength)
            buffer.position(buffer.position() + currencyLength)
            
            val activationDate = buffer.long
            val expirationDate = buffer.long
            
            return SatodimeStatus(
                isInitialized = (flags.toInt() and 0x01) != 0,
                isAuthenticated = (flags.toInt() and 0x02) != 0,
                isActivated = (flags.toInt() and 0x04) != 0,
                isSpent = (flags.toInt() and 0x08) != 0,
                denomination = denomination,
                currency = currency,
                activationDate = activationDate,
                expirationDate = expirationDate
            )
        }
        
        fun parseSatodimeKeyslotStatus(data: ByteArray): SatodimeKeyslotStatus {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            
            val flags = buffer.get()
            val denomination = buffer.get().toInt() and 0xFF
            val currencyLength = buffer.get().toInt() and 0xFF
            val currency = String(data, buffer.position(), currencyLength)
            buffer.position(buffer.position() + currencyLength)
            
            val activationDate = buffer.long
            val expirationDate = buffer.long
            
            val publicKeyLength = buffer.get().toInt() and 0xFF
            val publicKey = ByteArray(publicKeyLength)
            buffer.get(publicKey)
            
            return SatodimeKeyslotStatus(
                isInitialized = (flags.toInt() and 0x01) != 0,
                isAuthenticated = (flags.toInt() and 0x02) != 0,
                isActivated = (flags.toInt() and 0x04) != 0,
                isSpent = (flags.toInt() and 0x08) != 0,
                denomination = denomination,
                currency = currency,
                activationDate = activationDate,
                expirationDate = expirationDate,
                publicKey = publicKey
            )
        }
    }
} 