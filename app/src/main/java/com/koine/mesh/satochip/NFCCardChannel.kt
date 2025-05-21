package com.koine.mesh.satochip

import android.nfc.tech.IsoDep
import com.koine.mesh.satochip.client.CardChannel
import com.koine.mesh.satochip.client.CommandAPDU
import com.koine.mesh.satochip.client.ResponseAPDU
import java.io.IOException

class NFCCardChannel(private val isoDep: IsoDep) : CardChannel {
    override fun transmit(command: CommandAPDU): ResponseAPDU {
        return try {
            // Convert CommandAPDU to byte array
            val commandBytes = ByteArray(4 + command.data.size + (if (command.le > 0) 1 else 0))
            commandBytes[0] = command.cla
            commandBytes[1] = command.ins
            commandBytes[2] = command.p1
            commandBytes[3] = command.p2
            command.data.copyInto(commandBytes, 4)
            if (command.le > 0) {
                commandBytes[4 + command.data.size] = command.le.toByte()
            }
            
            // Send command and get response
            val responseBytes = isoDep.transceive(commandBytes)
            
            // Parse response into ResponseAPDU
            if (responseBytes.size < 2) {
                throw IOException("Invalid response from card")
            }
            
            val data = responseBytes.copyOfRange(0, responseBytes.size - 2)
            val sw1 = responseBytes[responseBytes.size - 2]
            val sw2 = responseBytes[responseBytes.size - 1]
            
            ResponseAPDU(data, sw1, sw2)
        } catch (e: IOException) {
            throw IOException("Failed to transmit command to NFC card", e)
        }
    }
} 