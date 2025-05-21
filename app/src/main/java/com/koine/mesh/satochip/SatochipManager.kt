package com.koine.mesh.satochip

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.koine.mesh.android.Logging
import com.koine.mesh.repository.SatochipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import com.koine.mesh.satochip.client.Constants

@Singleton
class SatochipManager @Inject constructor(
    private val context: Context,
    private val satochipRepository: SatochipRepository
) : Logging {
    private val nfcManager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
    private var nfcAdapter: NfcAdapter? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activityRef: WeakReference<Activity>? = null
    private var isoDep: IsoDep? = null
    private val secureChannel = SecureChannelSession()

    private val _isCardConnected = MutableStateFlow(false)
    val isCardConnected: StateFlow<Boolean> = _isCardConnected

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    fun setActivity(activity: Activity) {
        debug("SatochipManager: setActivity called with activity: ${activity.javaClass.simpleName}")
        activityRef = WeakReference(activity)
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
    }

    fun startNfcReader() {
        activityRef?.get()?.let { activity ->
            nfcAdapter?.enableReaderMode(
                activity,
                { tag ->
                    handleTag(tag)
                },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
            )
        }
    }

    fun stopNfcReader() {
        debug("SatochipManager: stopNfcReader called")
        val activity = activityRef?.get() ?: return
        try {
            nfcAdapter?.disableReaderMode(activity)
            isoDep?.close()
            isoDep = null
            _isCardConnected.value = false
            _isAuthenticated.value = false
            debug("SatochipManager: NFC reader stopped successfully")
        } catch (e: Exception) {
            errormsg("SatochipManager: stopNfcReader error: ${e.message}")
        }
    }

    private fun handleTag(tag: Tag) {
        try {
            isoDep = IsoDep.get(tag)
            isoDep?.connect()
            _isCardConnected.value = true

            // 1. Select Applet
            val selectApplet = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00, 0x08,
                0x53, 0x61, 0x74, 0x6F, 0x43, 0x68, 0x69, 0x70
            )
            var response = isoDep?.transceive(selectApplet)
            if (response == null || response.size < 2) {
                debug("handleTag: Failed to select applet")
                return
            }
            var sw = ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response[response.size - 1].toInt() and 0xFF)
            if (sw != 0x9000) {
                debug("handleTag: Applet selection failed, SW=${String.format("%04X", sw)}")
                return
            }
            debug("handleTag: Applet selected successfully")

            // 2. Get Card Status
            val getStatus = byteArrayOf(0xB0.toByte(), 0x3C.toByte(), 0x00, 0x00)
            response = isoDep?.transceive(getStatus)
            if (response == null || response.size < 2) {
                debug("handleTag: Failed to get card status")
                return
            }
            sw = ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response[response.size - 1].toInt() and 0xFF)
            if (sw != 0x9000) {
                debug("handleTag: Get card status failed, SW=${String.format("%04X", sw)}")
                return
            }
            debug("handleTag: Card status retrieved successfully")

            // 3. Initialize Secure Channel
            debug("handleTag: Starting secure channel initialization sequence")
            
            // First get the card's public key
            debug("handleTag: Requesting card's public key")
            val getPubkey = byteArrayOf(0xB0.toByte(), 0x02.toByte(), 0x00, 0x00)
            debug("handleTag: Get public key command: ${getPubkey.joinToString(" ") { "%02x".format(it) }}")
            
            response = isoDep?.transceive(getPubkey)
            if (response == null || response.size < 2) {
                debug("handleTag: Failed to get public key - null or empty response")
                return
            }
            sw = ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response[response.size - 1].toInt() and 0xFF)
            if (sw != 0x9000) {
                debug("handleTag: Get public key failed, SW=${String.format("%04X", sw)}")
                return
            }
            debug("handleTag: Get public key command successful")
            
            val pubkey = response.copyOfRange(0, response.size - 2)
            debug("handleTag: Public key retrieved, length=${pubkey.size}")
            debug("handleTag: Public key bytes: ${pubkey.joinToString(" ") { "%02x".format(it) }}")

            if (pubkey.size < 256) {  // RSA-2048 public key should be at least 256 bytes
                debug("handleTag: Invalid public key length: ${pubkey.size}")
                return
            }

            // Generate random challenge
            debug("handleTag: Generating random challenge")
            val challenge = ByteArray(32)
            java.security.SecureRandom().nextBytes(challenge)
            debug("handleTag: Generated challenge, length=${challenge.size}")
            debug("handleTag: Challenge bytes: ${challenge.joinToString(" ") { "%02x".format(it) }}")

            // Encrypt challenge with card's public key
            debug("handleTag: Encrypting challenge with public key")
            val encryptedChallenge = secureChannel.encryptWithPublicKey(challenge, pubkey)
            if (encryptedChallenge == null) {
                debug("handleTag: Failed to encrypt challenge with public key")
                return
            }
            debug("handleTag: Challenge encrypted successfully, length=${encryptedChallenge.size}")
            debug("handleTag: Encrypted challenge bytes: ${encryptedChallenge.joinToString(" ") { "%02x".format(it) }}")

            // Now initialize secure channel with encrypted challenge
            debug("handleTag: Initializing secure channel with encrypted challenge")
            val initSecureChannel = ByteArray(5 + encryptedChallenge.size)
            initSecureChannel[0] = 0xB0.toByte()
            initSecureChannel[1] = 0x81.toByte()
            initSecureChannel[2] = 0x00
            initSecureChannel[3] = 0x00
            initSecureChannel[4] = encryptedChallenge.size.toByte()
            System.arraycopy(encryptedChallenge, 0, initSecureChannel, 5, encryptedChallenge.size)

            debug("handleTag: Sending secure channel init command with data length=${encryptedChallenge.size}")
            debug("handleTag: Command bytes: ${initSecureChannel.joinToString(" ") { "%02x".format(it) }}")
            
            response = isoDep?.transceive(initSecureChannel)
            if (response == null || response.size < 2) {
                debug("handleTag: Failed to initialize secure channel - null or empty response")
                return
            }
            sw = ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response[response.size - 1].toInt() and 0xFF)
            if (sw == 0x9000) {
                val cardResponse = response.copyOfRange(0, response.size - 2)
                debug("handleTag: Received card response, length=${cardResponse.size}")
                debug("handleTag: Card response bytes: ${cardResponse.joinToString(" ") { "%02x".format(it) }}")
                
                secureChannel.initialize(cardResponse, challenge)
                debug("handleTag: Secure channel initialized successfully")
                _isAuthenticated.value = false  // Reset authentication state
            } else {
                debug("handleTag: Secure channel initialization failed, SW=${String.format("%04X", sw)}")
                return
            }

            // 4. Get Public Key (now that secure channel is initialized)
            val getPubkeyAgain = byteArrayOf(0xB0.toByte(), 0x73.toByte(), 0x00, 0x00)
            response = isoDep?.transceive(getPubkeyAgain)
            if (response == null || response.size < 2) {
                debug("handleTag: Failed to get public key again")
                return
            }
            sw = ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response[response.size - 1].toInt() and 0xFF)
            if (sw != 0x9000) {
                debug("handleTag: Get public key again failed, SW=${String.format("%04X", sw)}")
                return
            }
            val pubkeyAgain = response.copyOfRange(0, response.size - 2)
            debug("handleTag: Public key retrieved again successfully, length=${pubkeyAgain.size}")

        } catch (e: IOException) {
            errormsg("handleTag: IOException: ${e.message}")
            e.printStackTrace()
            _isCardConnected.value = false
        } catch (e: Exception) {
            errormsg("handleTag: Exception: ${e.message}")
            e.printStackTrace()
            _isCardConnected.value = false
        }
    }

    fun verifyPin(pin: String): Boolean {
        if (!_isCardConnected.value || isoDep == null) {
            debug("verifyPin: Card not connected or isoDep is null")
            return false
        }

        try {
            // Format PIN command
            val pinBytes = pin.toByteArray()
            debug("verifyPin: PIN length = ${pin.length}, pinBytes length = ${pinBytes.size}")
            debug("verifyPin: PIN (masked) = ****, pinBytes (hex) = " + pinBytes.joinToString(" ") { "%02x".format(it) })
            val command: ByteArray = ByteArray(5 + pinBytes.size)
            command[0] = Constants.CLA_SATOCHIP  // CLA = 0xB0
            command[1] = Constants.INS_VERIFY_PIN  // INS: VERIFY = 0x42
            command[2] = 0x00  // P1 PIN SLOT 0
            command[3] = 0x00  // P2 Unused
            command[4] = minOf(pinBytes.size, 255).toByte()  // Lc = Length of Command Data
            System.arraycopy(pinBytes, 0, command, 5, pinBytes.size)
            debug("verifyPin: APDU command = " + command.joinToString(" ") { "%02x".format(it) })

            // Send command
            val response = isoDep?.transceive(command)
            debug("verifyPin: Response = " + (response?.joinToString(" ") { "%02x".format(it) } ?: "null"))
            if (response != null && response.size >= 2) {
                val sw = ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response[response.size - 1].toInt() and 0xFF)
                debug("verifyPin: Status word (SW) = %04x".format(sw))
                if (sw == Constants.SW_SUCCESS) {
                    _isAuthenticated.value = true
                    debug("verifyPin: Authentication successful")
                    return true
                } else {
                    debug("verifyPin: Authentication failed, SW = %04x".format(sw))
                }
            } else {
                debug("verifyPin: Invalid or empty response from card")
            }
        } catch (e: IOException) {
            errormsg("verifyPin: IOException: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            errormsg("verifyPin: Exception: ${e.message}")
            e.printStackTrace()
        }
        return false
    }

    fun signMessage(message: String): ByteArray? {
        if (!_isAuthenticated.value || isoDep == null) {
            debug("signMessage: Not authenticated or card not connected")
            return null
        }

        try {
            val messageBytes = message.toByteArray()
            val MAX_CHUNK_SIZE = 200 // Maximum data size per APDU command (reduced to account for counter)
            
            // Split message into chunks if needed
            val chunks = messageBytes.toList().chunked(MAX_CHUNK_SIZE)
            
            var lastResponse: ByteArray? = null
            var counter = 3 // Start counter at 3 as shown in example
            
            for ((index, chunk) in chunks.withIndex()) {
                val isLastChunk = index == chunks.size - 1
                val chunkBytes = chunk.toByteArray()
                
                // Format message data with counter
                val messageData = ByteArray(2 + chunkBytes.size)
                messageData[0] = 0x00 // Message type
                messageData[1] = counter.toByte() // Counter
                System.arraycopy(chunkBytes, 0, messageData, 2, chunkBytes.size)
                
                // Format APDU command
                val command = ByteArray(5 + messageData.size)
                command[0] = Constants.CLA_SATOCHIP  // CLA = 0xB0
                command[1] = 0x82.toByte()  // INS: SIGN_MESSAGE
                command[2] = 0x00  // P1
                command[3] = 0x00  // P2
                command[4] = messageData.size.toByte()  // Lc
                System.arraycopy(messageData, 0, command, 5, messageData.size)
                
                debug("signMessage: Sending chunk ${index + 1}/${chunks.size}, size=${messageData.size}, counter=$counter")
                
                // Encrypt command if secure channel is initialized
                val encryptedCommand = if (secureChannel.isInitialized) {
                    secureChannel.encrypt(command)
                } else {
                    command
                }
                
                // Send command
                val response = isoDep?.transceive(encryptedCommand)
                if (response != null && response.size >= 2) {
                    val sw = ((response[response.size - 2].toInt() and 0xFF) shl 8) or (response[response.size - 1].toInt() and 0xFF)
                    
                    if (sw == 0x9000) {
                        // Decrypt response if secure channel is initialized
                        lastResponse = if (secureChannel.isInitialized) {
                            secureChannel.decrypt(response.copyOfRange(0, response.size - 2))
                        } else {
                            response.copyOfRange(0, response.size - 2)
                        }
                        
                        if (isLastChunk) {
                            debug("signMessage: Successfully signed message")
                            return lastResponse
                        }
                        counter++ // Increment counter for next chunk
                    } else {
                        debug("signMessage: Error response from card, SW=${String.format("%04X", sw)}")
                        return null
                    }
                } else {
                    debug("signMessage: Invalid or empty response from card")
                    return null
                }
            }
            
            return lastResponse
            
        } catch (e: IOException) {
            errormsg("signMessage: IOException: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            errormsg("signMessage: Exception: ${e.message}")
            e.printStackTrace()
        }
        return null
    }
}
