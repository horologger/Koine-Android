data class DataPacket(
    val dest: String,
    val channel: Int,
    val payload: String,
    val signature: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataPacket

        if (dest != other.dest) return false
        if (channel != other.channel) return false
        if (payload != other.payload) return false
        if (signature != null) {
            if (other.signature == null) return false
            if (!signature.contentEquals(other.signature)) return false
        } else if (other.signature != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dest.hashCode()
        result = 31 * result + channel
        result = 31 * result + payload.hashCode()
        result = 31 * result + (signature?.contentHashCode() ?: 0)
        return result
    }
} 