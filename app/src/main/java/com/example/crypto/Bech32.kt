package com.example.crypto

/**
 * Bech32 / SegWit address encoding (BIP-173).
 * Used for Bitcoin native SegWit (P2WPKH) addresses.
 */
object Bech32 {
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private val CHARSET_REV = IntArray(128) { -1 }.also { arr ->
        CHARSET.forEachIndexed { i, c -> arr[c.code] = i }
    }

    private fun polymod(values: IntArray): Int {
        var chk = 1
        val generator = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
        for (v in values) {
            val top = chk ushr 25
            chk = ((chk and 0x1ffffff) shl 5) xor v
            for (i in 0 until 5) {
                if ((top ushr i) and 1 != 0) chk = chk xor generator[i]
            }
        }
        return chk
    }

    private fun hrpExpand(hrp: String): IntArray {
        val out = IntArray(hrp.length * 2 + 1)
        for (i in hrp.indices) out[i] = hrp[i].code ushr 5
        out[hrp.length] = 0
        for (i in hrp.indices) out[hrp.length + 1 + i] = hrp[i].code and 31
        return out
    }

    private fun createChecksum(hrp: String, data: IntArray): IntArray {
        val values = hrpExpand(hrp) + data + IntArray(6)
        val mod = polymod(values) xor 1
        return IntArray(6) { (mod ushr (5 * (5 - it))) and 31 }
    }

    private fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): IntArray {
        var acc = 0
        var bits = 0
        val out = ArrayList<Int>()
        val maxv = (1 shl toBits) - 1
        for (b in data) {
            acc = (acc shl fromBits) or (b.toInt() and ((1 shl fromBits) - 1))
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                out.add((acc ushr bits) and maxv)
            }
        }
        if (pad) {
            if (bits > 0) out.add((acc shl (toBits - bits)) and maxv)
        } else {
            require(bits < fromBits) { "Excess padding" }
            require((acc shl (toBits - bits)) and maxv == 0) { "Non-zero padding" }
        }
        return out.toIntArray()
    }

    /** Encodes a segwit v0 P2WPKH address from a 20-byte witness program hash. */
    fun encodeSegwitV0(hrp: String, witnessProgram: ByteArray): String {
        require(witnessProgram.size == 20) { "P2WPKH witness program must be 20 bytes" }
        val data = intArrayOf(0) + convertBits(witnessProgram, 8, 5, true)
        val checksum = createChecksum(hrp, data)
        val sb = StringBuilder(hrp).append('1')
        (data + checksum).forEach { sb.append(CHARSET[it]) }
        return sb.toString()
    }

    /** Decodes a bech32 address into HRP and data. */
    fun decode(hrp: String, address: String): Pair<String, IntArray>? {
        val lower = address.lowercase()
        if (lower != address && lower != address.uppercase()) return null
        val pos = lower.lastIndexOf('1')
        if (pos < 1 || pos + 7 > lower.length) return null
        val foundHrp = lower.substring(0, pos)
        if (foundHrp != hrp.lowercase()) return null
        val dataPart = lower.substring(pos + 1)
        val data = IntArray(dataPart.length)
        for (i in dataPart.indices) {
            val v = if (dataPart[i].code < 128) CHARSET_REV[dataPart[i].code] else -1
            if (v < 0) return null
            data[i] = v
        }
        return if (verifyChecksum(hrp, data)) Pair(foundHrp, data) else null
    }

    private fun verifyChecksum(hrp: String, data: IntArray): Boolean {
        return polymod(hrpExpand(hrp) + data) == 1
    }
}
