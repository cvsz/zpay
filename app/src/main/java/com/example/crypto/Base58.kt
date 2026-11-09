package com.example.crypto

/**
 * Standard Base58 encoding/decoding (Bitcoin alphabet).
 * Used for Solana addresses, WIF keys, and other Bitcoin-derived encodings.
 */
object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    /** Encodes bytes to Base58 string. */
    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var zeros = 0
        while (zeros < input.size && input[zeros] == 0.toByte()) zeros++

        val size = ((input.size - zeros) * 138 / 100) + 1
        val b58 = ByteArray(size)
        var length = 0
        for (i in zeros until input.size) {
            var carry = input[i].toInt() and 0xFF
            var j = size - 1
            while ((carry != 0 || j > size - 1 - length) && j >= 0) {
                carry += 256 * b58[j].toInt()
                b58[j] = (carry % 58).toByte()
                carry /= 58
                j--
            }
            length = size - 1 - j
        }
        var start = size - length
        while (start < size && b58[start].toInt() == 0) start++
        val sb = StringBuilder()
        repeat(zeros) { sb.append('1') }
        for (i in start until size) sb.append(ALPHABET[b58[i].toInt()])
        return sb.toString()
    }

    /** Decodes Base58 string to bytes. */
    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)
        var zeros = 0
        while (zeros < input.length && input[zeros] == '1') zeros++
        val size = ((input.length - zeros) * 733 / 1000) + 1
        val b256 = ByteArray(size)
        var length = 0
        val indexes = IntArray(128) { -1 }.also { arr ->
            ALPHABET.forEachIndexed { i, c -> arr[c.code] = i }
        }
        for (c in input.substring(zeros)) {
            val idx = if (c.code < 128) indexes[c.code] else -1
            require(idx >= 0) { "Invalid base58 character: $c" }
            var carry = idx
            var j = size - 1
            while ((carry != 0 || j > size - 1 - length) && j >= 0) {
                carry += 58 * b256[j].toInt()
                b256[j] = (carry % 256).toByte()
                carry /= 256
                j--
            }
            length = size - 1 - j
        }
        var start = size - length
        while (start < size && b256[start].toInt() == 0) start++
        val result = ByteArray(zeros + (size - start))
        for (i in start until size) result[zeros + i - start] = b256[i]
        return result
    }
}
