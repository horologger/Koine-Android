package com.koine.mesh.repository

import com.koine.mesh.satochip.client.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SatochipRepository @Inject constructor() {
    private var commandSet: SatochipCommandSet? = null
    
    suspend fun initializeCard(cardChannel: CardChannel): Boolean = withContext(Dispatchers.IO) {
        try {
            commandSet = SatochipCommandSet(cardChannel)
            
            // Select the Satochip applet
            val selectResponse = commandSet?.selectApplet()
            if (selectResponse?.statusWord != Constants.SW_SUCCESS) {
                return@withContext false
            }
            
            // Get initial status
            val statusResponse = commandSet?.getStatus()
            if (statusResponse?.statusWord != Constants.SW_SUCCESS) {
                return@withContext false
            }
            
            // Parse application status
            val appStatus = SatochipParser.parseApplicationStatus(statusResponse.data)
            
            // If not initialized, initialize with default PIN
            if (!appStatus.isInitialized) {
                val initResponse = commandSet?.verifyPin(Constants.PIN_DEFAULT)
                if (initResponse?.statusWord != Constants.SW_SUCCESS) {
                    return@withContext false
                }
            }
            
            // Establish secure channel
            return@withContext commandSet?.establishSecureChannel() ?: false
        } catch (e: Exception) {
            return@withContext false
        }
    }
    
    suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = commandSet?.verifyPin(pin)
            return@withContext response?.statusWord == Constants.SW_SUCCESS
        } catch (e: Exception) {
            return@withContext false
        }
    }
    
    suspend fun changePin(oldPin: String, newPin: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = commandSet?.changePin(oldPin, newPin)
            return@withContext response?.statusWord == Constants.SW_SUCCESS
        } catch (e: Exception) {
            return@withContext false
        }
    }
    
    suspend fun getStatus(): SatochipParser.ApplicationStatus? = withContext(Dispatchers.IO) {
        try {
            val response = commandSet?.getStatus()
            if (response?.statusWord == Constants.SW_SUCCESS) {
                return@withContext SatochipParser.parseApplicationStatus(response.data)
            }
            return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }
    }
    
    suspend fun loadKey(keyType: Byte, keyData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = commandSet?.loadKey(keyType, keyData)
            return@withContext response?.statusWord == Constants.SW_SUCCESS
        } catch (e: Exception) {
            return@withContext false
        }
    }
    
    suspend fun deriveKey(keyType: Byte, path: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = commandSet?.deriveKey(keyType, path)
            return@withContext response?.statusWord == Constants.SW_SUCCESS
        } catch (e: Exception) {
            return@withContext false
        }
    }
    
    suspend fun sign(data: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val response = commandSet?.sign(data)
            if (response?.statusWord == Constants.SW_SUCCESS) {
                return@withContext response.data
            }
            return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }
    }
    
    suspend fun getPublicKey(keyType: Byte): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val response = commandSet?.getPublicKey(keyType)
            if (response?.statusWord == Constants.SW_SUCCESS) {
                return@withContext response.data
            }
            return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }
    }
    
    suspend fun encryptData(data: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        try {
            return@withContext commandSet?.encryptData(data)
        } catch (e: Exception) {
            return@withContext null
        }
    }
    
    suspend fun decryptData(data: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        try {
            return@withContext commandSet?.decryptData(data)
        } catch (e: Exception) {
            return@withContext null
        }
    }
} 