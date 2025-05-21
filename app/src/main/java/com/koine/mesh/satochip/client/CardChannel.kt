package com.koine.mesh.satochip.client

interface CardChannel {
    fun transmit(command: CommandAPDU): ResponseAPDU
}

data class CommandAPDU(
    val cla: Byte,
    val ins: Byte,
    val p1: Byte,
    val p2: Byte,
    val data: ByteArray = ByteArray(0),
    val le: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CommandAPDU
        return cla == other.cla &&
               ins == other.ins &&
               p1 == other.p1 &&
               p2 == other.p2 &&
               data.contentEquals(other.data) &&
               le == other.le
    }
    
    override fun hashCode(): Int {
        var result = cla.toInt()
        result = 31 * result + ins.toInt()
        result = 31 * result + p1.toInt()
        result = 31 * result + p2.toInt()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + le
        return result
    }
}

data class ResponseAPDU(
    val data: ByteArray,
    val sw1: Byte,
    val sw2: Byte
) {
    val statusWord: Int
        get() = ((sw1.toInt() and 0xFF) shl 8) or (sw2.toInt() and 0xFF)
        
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ResponseAPDU
        return data.contentEquals(other.data) && sw1 == other.sw1 && sw2 == other.sw2
    }
    
    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + sw1
        result = 31 * result + sw2
        return result
    }
} 